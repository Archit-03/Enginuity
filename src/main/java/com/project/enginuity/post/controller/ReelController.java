package com.project.enginuity.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.post.io.ReelRequest;
import com.project.enginuity.post.service.ReelService;
import com.project.enginuity.profile.security.CustomPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class ReelController {
    @Autowired
    private ReelService reelService;
    @PostMapping("/upload")
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
}
