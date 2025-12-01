package com.project.enginuity.profile.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.enginuity.profile.io.ProfileRequest;
import com.project.enginuity.profile.io.ProfileResponse;
import com.project.enginuity.profile.io.ProfileReviewResponse;
import com.project.enginuity.profile.io.ProfileUpdateRequest;
import com.project.enginuity.profile.security.CustomPrincipal;
import com.project.enginuity.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    @Autowired
    private ProfileService profileService;


    @PostMapping("/create")
    public ResponseEntity<?> createProfile(@AuthenticationPrincipal CustomPrincipal principal
    , @Valid @RequestPart("profile") String profile, @RequestPart("profilePic") MultipartFile picture){
        String userId= principal.getUserId();
        ObjectMapper objectMapper=new ObjectMapper();
        ProfileRequest profileRequest=null;
        try{
            profileRequest=objectMapper.readValue(profile,ProfileRequest.class);
            ProfileResponse profileResponse=profileService.createProfile(profileRequest,picture,userId);
            return new ResponseEntity<>(profileResponse,HttpStatus.CREATED);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error occurred while parsing json"+e.getMessage());
        }

    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getOthersProfile(@AuthenticationPrincipal CustomPrincipal principal,
                                        @PathVariable String username,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size){
        String currentUserId= principal.getUserId();
        ProfileResponse profileResponse=profileService.getOtherProfile(username,currentUserId,page,size);

        if (profileResponse!=null){
            return new ResponseEntity<>(profileResponse,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal CustomPrincipal principal,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size){
        String userId= principal.getUserId();
        ProfileResponse profileResponse=profileService.getMyProfile(userId,page,size);
        if (profileResponse!=null){
            return new ResponseEntity<>(profileResponse,HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/edit")
    public ResponseEntity<?> editProfile(@AuthenticationPrincipal CustomPrincipal principal,
    @Valid @RequestBody ProfileUpdateRequest profileUpdateRequest){
        String userId= principal.getUserId();
        ProfileResponse profileResponse=profileService.editProfile(profileUpdateRequest,userId);
        if (profileResponse!=null){
            return new ResponseEntity<>(profileResponse,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/edit-profilePic")
    public ResponseEntity<?> editProfilePic(@AuthenticationPrincipal CustomPrincipal principal,
                                            @RequestPart("image") MultipartFile image){
        String userId= principal.getUserId();
        profileService.editProfilePic(image,userId);
        return new ResponseEntity<>("Profile updated successfully!!",HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProfile(@AuthenticationPrincipal CustomPrincipal principal){
        String userId= principal.getUserId();
        profileService.deleteProfile(userId);
        return new ResponseEntity<>("Profile Deleted Successfully",HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProfilesByUsername(@RequestParam String username){
        List<ProfileReviewResponse> profiles=profileService.searchProfilesByUsername(username);
        if (profiles!=null && !profiles.isEmpty()){
            return new ResponseEntity<>(profiles,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @GetMapping("/check-username")
    public ResponseEntity<Map<String,Boolean>> isUsernameAvailable(@RequestParam String username){
        boolean available=profileService.checkUsernameAvailability(username);
        return  ResponseEntity.ok(Map.of("available",available));
    }

    @DeleteMapping("/delete-profilePic")
    public ResponseEntity<?> deleteProfilePic(@AuthenticationPrincipal CustomPrincipal principal){
        String userId= principal.getUserId();
        profileService.deleteProfilePic(userId);
        return new ResponseEntity<>("Profile Pic Deleted Successfully",HttpStatus.OK);
    }

}
