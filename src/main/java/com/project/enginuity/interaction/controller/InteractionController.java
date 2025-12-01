package com.project.enginuity.interaction.controller;

import com.project.enginuity.interaction.io.CommentRequest;
import com.project.enginuity.interaction.io.CommentResponse;
import com.project.enginuity.interaction.io.LikeResponse;
import com.project.enginuity.interaction.io.SaveResponse;
import com.project.enginuity.interaction.service.InteractionService;
import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.profile.security.CustomPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reels")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;
    @PostMapping("/{reelId}/like")
    public ResponseEntity<?> like(@AuthenticationPrincipal CustomPrincipal principal,
                                  @PathVariable String reelId){
        String userId = principal.getUserId();
        LikeResponse likeResponse= interactionService.toggleLike(userId,reelId);
        return new ResponseEntity<>(likeResponse, HttpStatus.OK);
    }
    @GetMapping("/{reelId}/likes/count")
    public ResponseEntity<Long> getLikesCount(@PathVariable String reelId) {
        return ResponseEntity.ok(interactionService.getLikesCount(reelId));
    }

    @PostMapping("/{reelId}/comments")
    public ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal CustomPrincipal principal,
                                                      @PathVariable String reelId,
                                                      @RequestBody CommentRequest req) {
        String userId = principal.getUserId();
        return ResponseEntity.ok(interactionService.addComment(userId, reelId, req.getComment()));
    }

    @GetMapping("/{reelId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(@PathVariable String reelId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(interactionService.getComments(reelId, page, size));
    }

    @DeleteMapping("/{reelId}/comment/{commentId}")
    public ResponseEntity<?> deleteComment(@AuthenticationPrincipal CustomPrincipal principal,
                                           @PathVariable String reelId,
                                           @PathVariable Long commentId
    ) {
        interactionService.deleteComment(principal.getUserId(), commentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @PostMapping("/{reelId}/save")
    public ResponseEntity<SaveResponse> toggleSave(@AuthenticationPrincipal CustomPrincipal principal,
                                                   @PathVariable String reelId) {
        String userId = principal.getUserId();
        return ResponseEntity.ok(interactionService.toggleSave(userId, reelId));
    }

    @GetMapping("/saved")
    public ResponseEntity<Page<ReelResponse>> getSaved(@AuthenticationPrincipal CustomPrincipal principal,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        String userId = principal.getUserId();
        return ResponseEntity.ok(interactionService.getSavedReels(userId, page, size));
    }
}



