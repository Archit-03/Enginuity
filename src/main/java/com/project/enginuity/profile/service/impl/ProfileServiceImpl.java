package com.project.enginuity.profile.service.impl;
import com.project.enginuity.profile.Exception.ProfileAlreadyExists;
import com.project.enginuity.profile.Exception.ProfileNotExists;
import com.project.enginuity.profile.Exception.UsernameAlreadyExists;
import com.project.enginuity.profile.io.ProfileRequest;
import com.project.enginuity.profile.io.ProfileResponse;
import com.project.enginuity.profile.io.ProfileReviewResponse;
import com.project.enginuity.profile.io.ProfileUpdateRequest;
import com.project.enginuity.profile.model.UserProfileEntity;
import com.project.enginuity.profile.repository.ProfileRepo;
import com.project.enginuity.profile.repository.UserRepo;
import com.project.enginuity.profile.service.ProfilePicService;
import com.project.enginuity.profile.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;
@Service
public class ProfileServiceImpl implements ProfileService {
    @Autowired
    private ProfileRepo profileRepo;
    @Autowired
    private ProfilePicService profilePicService;
    @Autowired
    private UserRepo userRepo;
    @Override
    public ProfileResponse createProfile(ProfileRequest profileRequest, MultipartFile profileUrl, String userId) {
        if (profileRepo.existsByUser_UserId(userId)){
            throw new ProfileAlreadyExists("You can not have more than one profile for individual user!!");
        }
        if (profileRepo.existsByUserNameIgnoreCase(profileRequest.getUsername())){
            throw new UsernameAlreadyExists("User with this username already exists!!");

        }
        String image=profilePicService.uploadImage(profileUrl);
        UserProfileEntity profile=convertToEntity(profileRequest);
        profile.setUser(userRepo.findByUserId(userId));
        profile.setProfilePictureUrl(image);
        profileRepo.save(profile);
        return convertToResponse(profile);
    }

    @Override
    public ProfileResponse getProfile(String userId) {
        UserProfileEntity profile=profileRepo.findByUser_UserId(userId);
        return convertToResponse(profile);
    }

    @Override
    public ProfileResponse editProfile(ProfileUpdateRequest profileUpdateRequest,String userId) {
        UserProfileEntity user=profileRepo.findByUser_UserId(userId);
        if (profileUpdateRequest.getUsername()!=null){
            if (profileRepo.existsByUserNameIgnoreCase(profileUpdateRequest.getUsername())){
                throw new UsernameAlreadyExists("User with this username already exists!!");
            }
            user.setUserName(profileUpdateRequest.getUsername());
        }
        if (profileUpdateRequest.getBio()!=null){
            user.setBio(profileUpdateRequest.getBio());
        }
        if (profileUpdateRequest.getGithubUrl()!=null){
            user.setGithubUrl(profileUpdateRequest.getGithubUrl());
        }
        if (profileUpdateRequest.getSkills()!=null){
            user.setSkills(profileUpdateRequest.getSkills());
        }
        if (profileUpdateRequest.getInterests()!=null){
            user.setInterests(profileUpdateRequest.getInterests());
        }
        profileRepo.save(user);
        return convertToResponse(user);
    }

    @Override
    public void editProfilePic(MultipartFile updatedProfile, String userId) {
        UserProfileEntity userProfile=profileRepo.findByUser_UserId(userId);
        String image=profilePicService.uploadImage(updatedProfile);
        userProfile.setProfilePictureUrl(image);
        profileRepo.save(userProfile);
    }

    @Override
    public void deleteProfile(String userId) {
        UserProfileEntity profile=profileRepo.findByUser_UserId(userId);
        profileRepo.delete(profile);
    }

    @Override
    public List<ProfileReviewResponse> searchProfilesByUsername(String username) {
        return profileRepo.findTop5ByUserNameContainingIgnoreCaseOrderByUserNameAsc(username.trim())
                .stream()
                .map(profile -> new ProfileReviewResponse(profile.getUserName(), profile.getProfilePictureUrl()))
                .toList();
    }



    @Override
    public ProfileResponse getProfileByUsername(String username) {
        UserProfileEntity profile=profileRepo.findByUserNameIgnoreCase(username)
                .orElseThrow(()->new ProfileNotExists("Profile not found with username: "+username));
        return convertToResponse(profile);
    }

    @Override
    public boolean checkUsernameAvailability(String username) {
        return !profileRepo.existsByUserNameIgnoreCase(username);
    }

    @Override
    public void deleteProfilePic(String userId) {
        UserProfileEntity userProfile=profileRepo.findByUser_UserId(userId);
        try {
            profilePicService.deleteProfile(userProfile.getProfilePictureUrl());
            userProfile.setProfilePictureUrl(null);
            profileRepo.save(userProfile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete profile picture", e);
        }
    }


    private UserProfileEntity convertToEntity(ProfileRequest profileRequest){
        return UserProfileEntity.builder()
                .profileId(UUID.randomUUID().toString())
                .userName(profileRequest.getUsername())
                .bio(profileRequest.getBio())
                .profilePictureUrl(profileRequest.getProfilePictureUrl())
                .skills(profileRequest.getSkills())
                .interests(profileRequest.getInterests())
                .githubUrl(profileRequest.getGithubUrl())
                .build();
    }

    private ProfileResponse convertToResponse(UserProfileEntity profile){
        return ProfileResponse.builder()
                .username(profile.getUserName())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .skills(profile.getSkills())
                .interests(profile.getInterests())
                .githubUrl(profile.getGithubUrl())
                .followerCount(profile.getFollowerCount())
                .build();
    }
}
