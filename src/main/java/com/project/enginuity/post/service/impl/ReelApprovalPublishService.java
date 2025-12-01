package com.project.enginuity.post.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.Exception.ReelUploadFailed;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.entity.ReelFileEntity;
import com.project.enginuity.post.io.ReelStatus;
import com.project.enginuity.post.repository.ReelFileRepo;
import com.project.enginuity.post.repository.ReelRepo;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ReelApprovalPublishService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired

    private ReelRepo reelRepo;

    @Autowired
    private ReelFileRepo reelFileRepo;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void publishApproved(ReelEntity reel, ReelFileEntity reelFile, Set<String> tags, String summary) throws IOException {
        String tempVideoPath = null;
        try{
            log.info("Publishing approved reel: {}", reel.getReelId());
            byte[] data = reelFile.getReelData();
            if (data == null || data.length == 0) {
                log.info("Reel file data is empty for reelId: {}", reel.getReelId());
                throw new ReelUploadFailed("Reel file data is empty for reelId: " , reel.getReelId());
            }
            log.info("Uploading reel to cloudinary for reelId: {}", reel.getReelId());
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().uploadLarge(data, ObjectUtils.asMap(
                    "resource_type", "video","public_id", reel.getReelId()
            ));
            String videoUrl = (String) uploadResult.get("secure_url");
            log.info("Reel uploaded to cloudinary: {} for reelId: {}", videoUrl, reel.getReelId());
            if (videoUrl == null || videoUrl.isEmpty()) {
                log.info("Failed to get video URL from cloudinary for reelId: {}", reel.getReelId());
                throw new ReelUploadFailed("Failed to get video URL from cloudinary for reelId: " , reel.getReelId());
            }

            // Generate thumbnail
            File tempVideoFile = File.createTempFile("reel_", ".mp4");
            tempVideoPath = tempVideoFile.getAbsolutePath();
            try (FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                fos.write(data);
                fos.flush();
            }
            BufferedImage image = null;
            try(FFmpegFrameGrabber frameGrabber= new FFmpegFrameGrabber(tempVideoFile)){
                frameGrabber.start();
                frameGrabber.setTimestamp(2_000_000); // 2 seconds
                Frame frame = frameGrabber.grabImage();
                if (frame==null){
                    long durationInMicroseconds=frameGrabber.getLengthInTime();
                    long middleTimestamp=durationInMicroseconds/2;
                    frameGrabber.setTimestamp(middleTimestamp);

                }
                frame = frameGrabber.grabImage();
                if (frame != null) {
                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    image = converter.getBufferedImage(frame);
                }else {
                    log.info("Failed to grab frame for thumbnail for reelId: {}", reel.getReelId());
                }
                frameGrabber.stop();
            }
            String thumbnailUrl = null;
            if (image != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();
                log.info("Uploading thumbnail to cloudinary for reelId: {}", reel.getReelId());
                @SuppressWarnings("unchecked")
                Map<String, Object> thumbUploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                        "resource_type", "image","public_id", reel.getReelId() + "_thumbnail"
                ));
                thumbnailUrl = (String) thumbUploadResult.get("secure_url");
                tempVideoFile.delete();
                log.info("Thumbnail uploaded to cloudinary: {} for reelId: {}", thumbnailUrl, reel.getReelId());
            } else {
                log.info("Thumbnail image is null for reelId: {}", reel.getReelId());

            }

            // Update reel entity
            log.info("Updating reel entity in database for reelId: {}", reel.getReelId());
            reel.setReelStatus(ReelStatus.ACCEPTED);
            reel.setReelUrl(videoUrl);
            reel.setThumbnailUrl(thumbnailUrl);
            reel.setTags((Set<String>) tags);
            reelRepo.save(reel);

            // Publish to Kafka
            log.info("Publishing reel approved event to Kafka for reelId: {}", reel.getReelId());

            Map<String, Object> eventPayload = Map.of(
                    "reelId", reel.getReelId(),
                    "userName", reel.getUser().getProfile().getUserName(),
                    "reelUrl", videoUrl,
                    "description", reel.getDescription(),
                    // createdAt as epoch seconds for easier sorting / cursor pagination
                    "createdAt", reel.getUpdatedAt() != null ? reel.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) : Instant.now().getEpochSecond(),
                    "thumbnailUrl", thumbnailUrl,
                    "tags", tags != null ? tags : Collections.emptyList()
            );

            String payload = objectMapper.writeValueAsString(eventPayload);
            for(String tag: tags){
                log.info("Reel tag: {} for reelId: {}", tag, reel.getReelId());
                CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("reels.accepted" , tag, payload);
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(" Sent reel event successfully for reelId: {} to partition: {}",
                                reel.getReelId(), result.getRecordMetadata().partition());
                    } else {
                        log.error("Failed to send reel event for reelId: {}", reel.getReelId(), ex);
                    }
                });
            }
        }catch (Exception e) {
            log.error("Error publishing approved reel for reelId {}: {}", reel.getReelId(), e.getMessage(), e);
            throw new ReelUploadFailed("Failed to publish approved reel", e.getMessage());
        } finally {
            // ensure temp file cleanup

            if (tempVideoPath != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempVideoPath));
                } catch (IOException ex) {
                    log.warn("Failed to delete temp video file {}: {}", tempVideoPath, ex.getMessage());
                }
            }

    }


}
}