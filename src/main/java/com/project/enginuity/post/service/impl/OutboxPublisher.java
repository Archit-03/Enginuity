package com.project.enginuity.post.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.entity.OutboxEventEntity;
import com.project.enginuity.post.io.OutboxStatus;
import com.project.enginuity.post.repository.OutBoxRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Clob;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {
    @Autowired
    private OutBoxRepo outBoxRepo;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    final private int MAX_RETRIES=3;
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEventEntity> outboxEvents = outBoxRepo.findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEventEntity event : outboxEvents) {
            try {
                kafkaTemplate.send("reels.uploaded",event.getPayload()).get();
                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(java.time.LocalDateTime.now());
                event.setRetryCount(event.getRetryCount() + 1);


            } catch (Exception e) {
                int retryCount = event.getRetryCount()+1;
                event.setRetryCount(retryCount);
                if (retryCount >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    event.setErrorMessage(e.getMessage().substring(0, Math.min(e.getMessage().length(), 300)));
                    sendToDlq(event);
                } else {
                    event.setStatus(OutboxStatus.PENDING);
                }


            }
            outBoxRepo.save(event);
        }


    }
    private void sendToDlq(OutboxEventEntity event){
        try{
            Map<String, Object> headers= Map.of(
                    "event-id", event.getEventId(),
                    "aggregate-id", event.getAggregateId(),
                    "event-type", event.getEventType(),
                    "error-message", event.getErrorMessage(),
                    "payload", event.getPayload(),
                    "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send("reels.uploaded.dlq", new ObjectMapper().writeValueAsString(headers));
        }catch (Exception e){
            //log error
            System.err.println("Failed to send event to DLQ: " + e.getMessage());
        }
    }




}
