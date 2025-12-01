package com.project.enginuity.post.service.impl;

import com.project.enginuity.post.io.ReelDLQ;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejectedReelDLQService {
    private final KafkaTemplate<String, ReelDLQ> kafkaTemplate;

    public void sendToDLQ(String reelId, String userId, String reason) {
        ReelDLQ dlqMessage = new ReelDLQ(
                reelId,
                userId,
                reason,
                System.currentTimeMillis()
        );

        kafkaTemplate.send("reel-upload-dlq", reelId, dlqMessage);
        System.out.println(" Reel moved to DLQ: " + reelId + " Reason: " + reason);
    }
}
