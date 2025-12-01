package com.project.enginuity.feed.controller;

import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.feed.service.FeedService;
import com.project.enginuity.profile.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final JwtUtils jwtUtils;
    private final Logger log = LoggerFactory.getLogger(FeedController.class);

    // Extract userId from JWT
    private String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtUtils.extractUserId(token);
    }

    // -------------------------------------------
    // 1) FORCE REBUILD USER FEED
    // -------------------------------------------
    @PostMapping("/build")
    public ResponseEntity<String> buildFeed(@RequestHeader("Authorization") String auth) {
        String userId = extractUserId(auth);
        feedService.buildUserFeed(userId);
        return ResponseEntity.ok("Feed built successfully for user: " + userId);
    }

    // -------------------------------------------
    // 2) GET FEED PAGE (cursor-based pagination)
    // -------------------------------------------
    @GetMapping
    public ResponseEntity<List<ReelResponse>> getFeedPage(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false, defaultValue = "9999999999999") double beforeScore,
            @RequestParam(required = false) String beforeId,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = extractUserId(auth);

        log.debug("Fetching feed page for user {}, beforeScore {}, beforeId {}",
                userId, beforeScore, beforeId);

        List<ReelResponse> page = feedService.getFeedPageCursor(
                userId,
                beforeScore,
                beforeId,
                size
        );
        return ResponseEntity.ok(page);
    }

    // -------------------------------------------
    // 3) MARK REEL AS SEEN
    // -------------------------------------------
    @PostMapping("/seen/{reelId}")
    public ResponseEntity<String> markSeen(
            @RequestHeader("Authorization") String auth,
            @PathVariable String reelId
    ) {
        String userId = extractUserId(auth);
        feedService.markSeen(userId, reelId);
        return ResponseEntity.ok("Reel marked as seen: " + reelId);
    }
}
