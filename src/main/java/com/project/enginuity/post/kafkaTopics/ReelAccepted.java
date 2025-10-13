package com.project.enginuity.post.kafkaTopics;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ReelAccepted {
    @Bean
    public NewTopic reelsAcceptedTopic(){
        return new NewTopic("reels.accepted",10,(short)1);
    }
}
