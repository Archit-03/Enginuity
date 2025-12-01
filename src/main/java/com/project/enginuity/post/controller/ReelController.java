package com.project.enginuity.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.io.ReelRequest;
import com.project.enginuity.post.service.ReelService;
import com.project.enginuity.profile.security.CustomPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReelController {
    @Autowired
    private ReelService reelService;
    @PostMapping("/reel/upload")
    public ResponseEntity<?> uploadReel(@AuthenticationPrincipal CustomPrincipal principal, @RequestPart("reel") String reel,@RequestPart("file") MultipartFile file){

        String userId=principal.getUserId();
        ObjectMapper objectMapper=new ObjectMapper();
        ReelRequest reelRequest=null;
        try{
            reelRequest=objectMapper.readValue(reel, ReelRequest.class);
            reelService.uploadReel(userId,reelRequest,file);


        }catch (Exception e){
            return ResponseEntity.badRequest().body("Error uploading reel: "+e.getMessage());
        }
        return ResponseEntity.ok("Reel uploaded successfully by user: "+userId);
    }

    @GetMapping("/reel/{reelId}")
    public ResponseEntity<?> getReelById(@PathVariable String reelId){
        return ResponseEntity.ok(reelService.getReelById(reelId));
    }

    @DeleteMapping("/reel/{reelId}/delete")
    public ResponseEntity<?> deleteReel(@AuthenticationPrincipal CustomPrincipal customPrincipal,
                                        @PathVariable String reelId){
        reelService.deleteReel(customPrincipal.getUserId(), reelId);
        return new ResponseEntity<>("Reel deleted successfully",HttpStatus.OK);
    }
}
