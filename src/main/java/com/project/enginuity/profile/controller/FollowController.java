package com.project.enginuity.profile.controller;

import com.project.enginuity.profile.security.CustomPrincipal;
import com.project.enginuity.profile.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowController {
    @Autowired
    private FollowService followService;
    @PostMapping("/{targetUsername}")
    public ResponseEntity<String> followUser(@AuthenticationPrincipal CustomPrincipal principal,
                                             @PathVariable String targetUsername) {
        String userId= principal.getUserId();
        followService.followUser(userId,targetUsername);
        return ResponseEntity.ok("Followed " + targetUsername);
    }
    @DeleteMapping("/{targetUsername}")
    public ResponseEntity<String> unfollowUser(@AuthenticationPrincipal CustomPrincipal principal,
                                             @PathVariable String targetUsername) {
        String userId= principal.getUserId();
        followService.unfollowUser(userId,targetUsername);
        return ResponseEntity.ok("Unfollowed " + targetUsername);
    }
}
