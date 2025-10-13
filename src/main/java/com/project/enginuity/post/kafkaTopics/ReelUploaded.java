package com.project.enginuity.post.kafkaTopics;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReelUploaded {

    @Bean
    public NewTopic reelsUploadedTopic(){
        return new NewTopic("reels.uploaded",5,(short)1);
    }
}
