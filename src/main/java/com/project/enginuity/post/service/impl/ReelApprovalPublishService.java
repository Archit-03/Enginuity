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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReelApprovalPublishService {
    ObjectMapper objectMapper=new ObjectMapper();
    @Autowired
    private ReelRepo reelRepo;
    @Autowired
    private ReelFileRepo reelFileRepo;
    @Autowired
    private Cloudinary cloudinary;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ThumbnailService thumbnailService;

    public void publishApproved(ReelEntity reel, ReelFileEntity reelFile, List<String> tags,String summary) throws IOException {
        try{
            Map uploadResult=cloudinary
                    .uploader()
                    .upload(new ByteArrayInputStream(reelFile.getReelData()),
                            ObjectUtils.asMap("resource_type","video",
                                    "public_id",reel.getReelId())
                    );
            String videoUrl= (String) uploadResult.get("secure_url");
            byte[] thumbnailData=thumbnailService.generateThumbnail(reelFile.getReelData());
            String thumbnailUrl=null;
            if(thumbnailData!=null){
                Map thumbUploadResult=cloudinary
                        .uploader()
                        .upload(new ByteArrayInputStream(thumbnailData),
                                ObjectUtils.asMap("resource_type","image",
                                        "public_id",reel.getReelId()+"_thumb")
                        );
                thumbnailUrl=(String) thumbUploadResult.get("secure_url");
            }

            reel.setReelUrl(videoUrl);
            reel.setThumbnailUrl(thumbnailUrl);
            reel.setReelStatus(ReelStatus.ACCEPTED);
            reel.setTags(tags!=null ? Set.copyOf(tags) : Set.of());
            reel.setSummary(summary);
            reelRepo.save(reel);

            reelFileRepo.delete(reelFile);

            // Publish event
            Map<String,Object> eventPayload= Map.of(
                     "reelId",reel.getReelId(),
                     "reelUrl",videoUrl,
                     "thumbnailUrl",thumbnailUrl,
                     "tags",reel.getTags(),
                     "description",reel.getDescription()
           );

            for(String tag:tags){
                kafkaTemplate.send("reels.accepted",tag,objectMapper.writeValueAsString(eventPayload));
            }

        }catch (Exception e){
            throw new ReelUploadFailed("Failed to publish approved reel",e.getMessage());

        }
    }

}
