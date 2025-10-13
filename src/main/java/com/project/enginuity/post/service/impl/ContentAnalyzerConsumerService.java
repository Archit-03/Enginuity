package com.project.enginuity.post.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.Exception.ReelNotFoundException;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.entity.ReelFileEntity;
import com.project.enginuity.post.io.AnalyzerRequest;
import com.project.enginuity.post.io.AnalyzerResponse;
import com.project.enginuity.post.repository.ReelFileRepo;
import com.project.enginuity.post.repository.ReelRepo;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

@Service
public class ContentAnalyzerConsumerService {
    ObjectMapper objectMapper= new ObjectMapper();
    @Autowired
    private ReelRepo reelRepo;
    @Autowired
    private ReelFileRepo reelFileRepo;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    @Autowired
    private WebClient webClient;
    @Autowired
    private ReelApprovalPublishService reelApprovalPublishService;
    @Transactional
    @KafkaListener(topics="reels.uploaded",groupId = "content-analyzer-group",concurrency = "5")
    public void consume(ConsumerRecord<String,String> record){
        try{
            Map payload= objectMapper.readValue(record.value(),Map.class);
            String reelId= (String) payload.get("reelId");
            ReelEntity reel=reelRepo.findByReelId(reelId);
            ReelFileEntity reelFile=reelFileRepo.findByReelId(reelId);
            // Simulate content analysis
            if (reel==null || reelFile==null){
                throw new ReelNotFoundException("Reel or Reel File not found for reelId: "+reelId);
            }

            AnalyzerRequest analyzerRequest=new AnalyzerRequest();
            analyzerRequest.setReelId(reelId);
            analyzerRequest.setDescription(reel.getDescription());
            analyzerRequest.setReelData(reelFile.getReelData());

            AnalyzerResponse analyzerResponse=webClient.post()
                    .uri("http://localhost:5000/analyze")
                    .bodyValue(analyzerRequest)
                    .retrieve()
                    .bodyToMono(AnalyzerResponse.class)
                    .block();
            if (analyzerResponse==null){
                System.out.println("Analyzer service returned null response for reelId: "+reelId);
            }

            if (analyzerResponse.isApproved()){
                reelApprovalPublishService.publishApproved(reel,reelFile,analyzerResponse.getTags(),analyzerResponse.getSummary());
            }else {

            }




        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
