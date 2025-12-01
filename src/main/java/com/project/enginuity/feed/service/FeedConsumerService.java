package com.project.enginuity.feed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.*;
import org.slf4j.*;

import java.time.Instant;
import java.util.*;

@Service
public class FeedConsumerService {

    private final Logger log = LoggerFactory.getLogger(FeedConsumerService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Keys
    private static final String PROCESSED_SET = "processedReels";
    private static final String TAG_FEED_PREFIX = "feed:tag:"; // Sorted set: reelId -> score (timestamp)
    private static final long TAG_FEED_MAX_SIZE = 20_000; // tuning

    public FeedConsumerService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "reels.accepted", groupId = "feed-consumer-group", concurrency = "10")
    public void listen(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String reelId = node.get("reelId").asText();
            // dedupe
            Boolean processed = redisTemplate.opsForSet().isMember(PROCESSED_SET, reelId);
            if (Boolean.TRUE.equals(processed)) {
                log.debug("Skipping already processed reel {}", reelId);
                return;
            }
            // mark processed
            redisTemplate.opsForSet().add(PROCESSED_SET, reelId);

            // get tags array (if present)
            Set<String> tags = new HashSet<>();
            if (node.has("tags") && node.get("tags").isArray()) {
                for (JsonNode t : node.get("tags")) {
                    tags.add(t.asText().toLowerCase());
                }
            } else if (node.has("tag")) {
                tags.add(node.get("tag").asText().toLowerCase());
            }

            long score = Instant.now().toEpochMilli();
            for (String tag : tags) {
                String key = TAG_FEED_PREFIX + tag;
                // zadd with score
                redisTemplate.opsForZSet().add(key, reelId, score);
                // trim the ZSET to max allowed
                redisTemplate.opsForZSet().removeRange(key, 0, -TAG_FEED_MAX_SIZE - 1);
                log.info("Added reel {} to Redis tag feed {}", reelId, key);
            }

            // if no tags, put into a generic "trending" tag
            if (tags.isEmpty()) {
                String key = TAG_FEED_PREFIX + "trending";
                redisTemplate.opsForZSet().add(key, reelId, score);
            }

        } catch (Exception e) {
            log.error("Failed to process reel event: {}", e.getMessage(), e);
        }
    }
}
