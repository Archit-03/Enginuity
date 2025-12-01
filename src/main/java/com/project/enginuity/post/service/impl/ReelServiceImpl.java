package com.project.enginuity.post.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.Exception.*;
import com.project.enginuity.post.entity.OutboxEventEntity;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.entity.ReelFileEntity;
import com.project.enginuity.post.io.ReelRequest;
import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.post.io.ReelStatus;
import com.project.enginuity.post.repository.OutBoxRepo;
import com.project.enginuity.post.repository.ReelFileRepo;
import com.project.enginuity.post.repository.ReelRepo;
import com.project.enginuity.post.service.ReelService;
import com.project.enginuity.profile.model.UserEntity;
import com.project.enginuity.profile.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.mp4parser.IsoFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReelServiceImpl implements ReelService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ReelRepo reelRepo;
    @Autowired
    private OutBoxRepo outBoxRepo;
    @Autowired
    private ReelFileRepo reelFileRepo;

    @Autowired
    private ProhibitedWordService prohibitedWordService;

    private double duration;


    @Override
    @Transactional
    public void uploadReel(String userId, ReelRequest reelRequest, MultipartFile file) throws IOException {
        if(prohibitedWordService.containsProhibitedWord(reelRequest.getDescription())){
            throw new ProhibitedWordsException("Reel description contains prohibited words.");
        }
        validateReel(file);
        UserEntity user=userRepo.findByUserId(userId);
        ReelEntity reel = new ReelEntity();
        reel.setReelId(UUID.randomUUID().toString());
        reel.setUser(user);
        reel.setDescription(reelRequest.getDescription());
        reel.setReelStatus(ReelStatus.UPLOADED);
        reel.setDurationInSeconds((int) duration);
        reelRepo.save(reel);

        ReelFileEntity reelFile = new ReelFileEntity();
        reelFile.setReelId(reel.getReelId());
        reelFile.setReelData(file.getBytes());
        reelFileRepo.save(reelFile);

        try{
            OutboxEventEntity outboxEvent = new OutboxEventEntity();
            outboxEvent.setEventId(UUID.randomUUID().toString());
            outboxEvent.setAggregateId(reel.getReelId());
            outboxEvent.setAggregateType("Reel");
            outboxEvent.setEventType("Reel_Uploaded");
            Map<String,Object> payload=new HashMap<>();
            payload.put("reelId", reel.getReelId());
            payload.put("userId", userId);
            payload.put("description", reel.getDescription());
            outboxEvent.setPayload(new ObjectMapper().writeValueAsString(payload));
            outBoxRepo.save(outboxEvent);



        }catch (Exception e){
            throw new RuntimeException("Failed to create outbox event: " + e.getMessage());
        }
    }

    @Override
    public ReelResponse getReelById(String reelId) {
        ReelEntity reel = reelRepo.findByReelId(reelId);
        if (reel==null){
            throw new ReelNotFoundException("Reel with this reelId"+reelId+" not found");
        }else{
            return new ReelResponse(reel.getReelId(),reel.getUser().getProfile().getUserName(),reel.getReelUrl(),reel.getThumbnailUrl(),reel.getDescription(),reel.getCreatedAt().toString());
        }
    }

    @Override
    public void deleteReel(String userId, String reelId) {
        ReelEntity reel = reelRepo.findByReelId(reelId);
        if (!reel.getUser().getUserId().equals(userId)){
            throw new RuntimeException("User can not delete other user's reel");
        }
        else{
            reelRepo.delete(reel);
        }
    }

    private void validateReel(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if(contentType == null || (!contentType.equals("video/mp4"))){
            throw new InvalidFileTypeException("Invalid file type. Only MP4 videos are allowed.");
        }

        long maxSize = 45 * 1024 * 1024; // 45 MB
        if(file.getSize() > maxSize){
            throw new FileSizeLimitExceededException("File size exceeds the maximum limit of 45 MB.");
        }

        File tempFile = null;
        try {
            // ✅ Step 1: Create a dedicated copy, not Tomcat’s internal temp file
            tempFile = Files.createTempFile("reel_", ".mp4").toFile();

            try (InputStream in = file.getInputStream();
                 OutputStream out = new FileOutputStream(tempFile)) {
                in.transferTo(out); // copy stream safely
            }

            // ✅ Step 2: Use IsoFile on our own temp copy
            try (FileChannel fc = new FileInputStream(tempFile).getChannel()) {
                IsoFile isoFile = new IsoFile(fc);
                 duration = (double) isoFile.getMovieBox().getMovieHeaderBox().getDuration() /
                        isoFile.getMovieBox().getMovieHeaderBox().getTimescale();

                if (duration > 60 || duration < 30) {
                    throw new ReelDurationException("Video duration must be between 30 and 60 seconds.");
                }
            }

        } catch (Exception e) {
            throw new InvalidVideoException("Error processing video: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }


    }

    public ReelResponse mapToReelResponse(ReelEntity reelEntity){

        return new ReelResponse(reelEntity.getReelId(),
                reelEntity.getUser().getProfile().getUserName(),
                reelEntity.getReelUrl(),
                reelEntity.getThumbnailUrl(),
                reelEntity.getDescription(),
                reelEntity.getCreatedAt().toString());
    }


}
