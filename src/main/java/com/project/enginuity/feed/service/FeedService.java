package com.project.enginuity.feed.service;



import com.project.enginuity.post.repository.ReelRepo;
import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.post.entity.ReelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FeedService (ZSET-based)
 *
 * Assumptions:
 * - ReelRepository has: List<ReelEntity> findAllByReelIdIn(Collection<String> ids)
 * - ReelEntity has getReelId() and other metadata fields used to build ReelResponse
 * - ReelResponse is your API DTO to send to clients
 */
@Service
public class FeedService {

    private final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final RedisTemplate<String, String> redis;
    private final ZSetOperations<String, String> zSetOps;
    private final SetOperations<String, String> setOps;
    private final ReelRepo reelRepository;

    // Tunables
    private static final int PER_TAG_FETCH = 50;      // top N to fetch per tag when building a feed
    private static final long MAX_FEED_SIZE = 10_000; // max items to keep in per-user feed
    private static final Duration USER_FEED_TTL = Duration.ofMinutes(5);

    // Key patterns
    private static final String TAG_FEED_PREFIX = "feed:tag:";     // ZSET (reelId -> score)
    private static final String USER_FEED_PREFIX = "feed:user:";   // ZSET (reelId -> score)
    private static final String USER_INTERESTS_KEY = "user:interests:"; // SET of tags
    private static final String USER_SEEN_PREFIX = "user:seen:";   // SET of seen reelIds for user

    public FeedService(RedisTemplate<String, String> redis, ReelRepo reelRepository) {
        this.redis = redis;
        this.zSetOps = redis.opsForZSet();
        this.setOps = redis.opsForSet();
        this.reelRepository = reelRepository;
    }

    // -------------------------
    // Producer-side helper
    // Call this when a new reel is uploaded (e.g. from your Kafka consumer)
    // -------------------------
    public void addReelToTagFeeds(String reelId, Set<String> tags, long timestampMillis) {
        if (tags == null || tags.isEmpty()) {
            tags = Collections.singleton("trending");
        }
        final double score = (double) timestampMillis;
        for (String tag : tags) {
            String key = TAG_FEED_PREFIX + tag.toLowerCase();
            zSetOps.add(key, reelId, score);
            // trim to keep only the newest MAX (keep rightmost newest when using reverseRange)
            zSetOps.removeRange(key, 0, - (20_000 + 1)); // keep newest 20k (tuning)
        }
    }

    // -------------------------
    // Build per-user feed (ZSET)
    // Merges top PER_TAG_FETCH from each interest and writes into feed:user:{userId}
    // -------------------------
    @Transactional
    public void buildUserFeed(String userId) {
        String userFeedKey = USER_FEED_PREFIX + userId;

        log.debug("Building feed for user {}", userId);

        // Clear existing feed (we'll rebuild)
        redis.delete(userFeedKey);

        // Get user interests from a Set in Redis. If absent, fallback to "trending"
        String interestsKey = USER_INTERESTS_KEY + userId;
        Set<String> interests = setOps.members(interestsKey);
        if (interests == null || interests.isEmpty()) {
            interests = Set.of("trending");
        }

        // Priority queue to merge by score descending (newest first)
        PriorityQueue<ReelScore> pq = new PriorityQueue<>(Comparator.comparingLong(rs -> -rs.score));

        for (String tag : interests) {
            String tagKey = TAG_FEED_PREFIX + tag.toLowerCase();
            Set<ZSetOperations.TypedTuple<String>> tuples = zSetOps.reverseRangeWithScores(tagKey, 0, PER_TAG_FETCH - 1);
            if (tuples == null || tuples.isEmpty()) continue;
            for (ZSetOperations.TypedTuple<String> t : tuples) {
                if (t == null) continue;
                String rId = t.getValue();
                Double s = t.getScore();
                long score = (s == null) ? 0L : s.longValue();
                pq.offer(new ReelScore(rId, score));
            }
        }

        // Exclude seen reels
        String seenKey = USER_SEEN_PREFIX + userId;
        Set<String> seen = setOps.members(seenKey); // may be null

        // Use LinkedHashSet to keep insertion order (which will be newest → oldest after poll)
        List<ReelScore> merged = new ArrayList<>();


        while (!pq.isEmpty() && merged.size() < MAX_FEED_SIZE) {
            ReelScore rs = pq.poll();
            if (rs == null) break;
            if (seen != null && seen.contains(rs.reelId)) continue;
            merged.add(rs);

        }

        if (!merged.isEmpty()) {
            // Add each entry into user feed ZSET with its original score (timestamp)
            // We already have timestamp in pq entries; but we lost them when iterating merged.
            // For simplicity, fetch scores from tag ZSETs again or keep a map - here we will add with current time fallback
            // Simpler approach: while merging, we can store mapping — but to keep code compact we'll add with current time.
            // Better: store ReelScore objects in 'merged' step with score; for now, we re-push with current timestamp to keep ordering correct.
            long now = System.currentTimeMillis();
            for (ReelScore rs : merged) {
                zSetOps.add(userFeedKey, rs.reelId, (double) rs.score);
            }


            // Trim to MAX_FEED_SIZE
            zSetOps.removeRange(userFeedKey, 0, -(int)MAX_FEED_SIZE - 1);


            // Set TTL
            redis.expire(userFeedKey, USER_FEED_TTL);
        } else {
            // Empty feed -> set short TTL to avoid rebuild spam
            redis.expire(userFeedKey, Duration.ofSeconds(30));
        }

        log.debug("Built feed for {} with {} items", userId, merged.size());
    }



    // -------------------------
    // Cursor-based pagination (recommended)
    // Provide lastSeenScore (exclusive) and optionally lastSeenId to handle equal scores.
    // Example usage:
    //  - first page: pass beforeScore = Double.POSITIVE_INFINITY
    //  - subsequent pages: pass beforeScore = lastReturnedScore (exclusive)
    // -------------------------
    public List<com.project.enginuity.post.io.ReelResponse> getFeedPageCursor(String userId, double beforeScoreExclusive, String beforeId, int pageSize) {
        String feedKey = USER_FEED_PREFIX + userId;
        Boolean exists = redis.hasKey(feedKey);
        if (exists == null || !exists) {
            buildUserFeed(userId);
        }

        // use reverseRangeByScore to get newest items with score < beforeScoreExclusive
        // we subtract a tiny epsilon to make it exclusive if needed, but Redis supports exclusive bounds in commands;
        // in Spring's API we can use reverseRangeByScoreWithScores(max, min, offset, count)
        double max = beforeScoreExclusive;
        double min = 0d; // oldest

        // page fetch
        Set<ZSetOperations.TypedTuple<String>> tuples =
                zSetOps.reverseRangeByScoreWithScores(feedKey, max, min, 0, pageSize);

        if (tuples == null || tuples.isEmpty()) return Collections.emptyList();

        List<String> ids = tuples.stream().map(ZSetOperations.TypedTuple::getValue).collect(Collectors.toList());
        return batchFetchMetadataPreserveOrder(ids);
    }

    // -------------------------
    // Mark reel seen: add to seen set and remove from user feed ZSET
    // -------------------------
    public void markSeen(String userId, String reelId) {
        String seenKey = USER_SEEN_PREFIX + userId;
        setOps.add(seenKey, reelId);

        String userFeedKey = USER_FEED_PREFIX + userId;
        zSetOps.remove(userFeedKey, reelId);
    }

    // -------------------------
    // Batch DB metadata fetch preserving order of IDs
    // -------------------------
    private List<com.project.enginuity.post.io.ReelResponse> batchFetchMetadataPreserveOrder(List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) return Collections.emptyList();

        List<com.project.enginuity.post.entity.ReelEntity> entities = reelRepository.findAllByReelIdIn(orderedIds);
        if (entities == null || entities.isEmpty()) return Collections.emptyList();

        // map by id
        Map<String, ReelEntity> map = entities.stream()
                .collect(Collectors.toMap(ReelEntity::getReelId, e -> e));

        // preserve order
        List<ReelResponse> result = new ArrayList<>(orderedIds.size());
        for (String id : orderedIds) {
            ReelEntity e = map.get(id);
            if (e == null) continue;
            result.add(mapEntityToDto(e));
        }
        return result;
    }

    private ReelResponse mapEntityToDto(ReelEntity e) {
        // adapt to your ReelResponse constructor
        return new ReelResponse(
                e.getReelId(),
                e.getUser().getUserId(),
                e.getDescription() == null ? "" : e.getDescription(),
                e.getReelUrl(),
                e.getThumbnailUrl(),
                e.getCreatedAt() == null ? "" : String.valueOf(e.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC))
        );
    }

    // Internal helper
    private static class ReelScore {
        final String reelId;
        final long score;
        ReelScore(String r, long s) { this.reelId = r; this.score = s; }
    }
}
