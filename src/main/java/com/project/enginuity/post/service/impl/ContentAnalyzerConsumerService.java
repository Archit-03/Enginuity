package com.project.enginuity.post.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.Exception.ReelNotFoundException;
import com.project.enginuity.post.Exception.ReelUploadFailed;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.entity.ReelFileEntity;
import com.project.enginuity.post.io.AnalyzerRequest;
import com.project.enginuity.post.io.AnalyzerResponse;
import com.project.enginuity.post.io.ReelStatus;
import com.project.enginuity.post.repository.ReelFileRepo;
import com.project.enginuity.post.repository.ReelRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.internals.Acknowledgements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class ContentAnalyzerConsumerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ReelRepo reelRepo;

    @Autowired
    private ReelFileRepo reelFileRepo;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ReelApprovalPublishService reelApprovalPublishService;

    @Autowired
    private RejectedReelDLQService rejectedReelDLQService;

    @Transactional
    @KafkaListener(topics = "reels.uploaded", groupId = "analyzer-group", concurrency = "5")
    public void consume(ConsumerRecord<String, String> record) {
        String reelId = null;
        try {
            log.info("Received Kafka message: {}", record.value());

            Map<String, Object> payload = objectMapper.readValue(record.value(), Map.class);
            reelId = (String) payload.get("reelId");

            ReelEntity reel = reelRepo.findByReelId(reelId);
            ReelFileEntity reelFile = reelFileRepo.findByReelId(reelId);

            if (reel == null || reelFile == null) {
                throw new ReelNotFoundException("Reel or Reel File not found for reelId: " + reelId);
            }

            AnalyzerRequest analyzerRequest = new AnalyzerRequest();
            analyzerRequest.setReelId(reelId);
            analyzerRequest.setDescription(reel.getDescription());
            analyzerRequest.setReelData(reelFile.getReelData());

            AnalyzerResponse analyzerResponse = webClient.post()
                    .uri("http://localhost:5000/analyze-reel")
                    .bodyValue(analyzerRequest)
                    .retrieve()
                    .bodyToMono(AnalyzerResponse.class)
                    .block();

            if (analyzerResponse == null) {
                log.warn("Analyzer service returned null response for reelId: {}", reelId);
                return;
            }

            if (analyzerResponse.isApproved()) {
                reelApprovalPublishService.publishApproved(reel, reelFile, analyzerResponse.getTags(), analyzerResponse.getSummary());
            } else {
                reel.setReelStatus(ReelStatus.REJECTED);
                reelFileRepo.delete(reelFile);
                rejectedReelDLQService.sendToDLQ(reelId,reel.getUser().getUserId(),"Content is not tech related and it contains irrelevant content");
                log.info("Reel rejected by analyzer for reelId: {}", reelId);

            }

        } catch (ReelNotFoundException e) {
            log.warn("Reel not found: {}", e.getMessage());
        } catch (ReelUploadFailed e) {
            log.error("Reel upload failed for reelId {}: {}", reelId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while consuming reelId {}: {}", reelId, e.getMessage(), e);
        }
    }
}