package com.project.enginuity.profile.service.impl;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.post.repository.ReelRepo;
import com.project.enginuity.post.service.ReelService;
import com.project.enginuity.post.service.impl.ReelServiceImpl;
import com.project.enginuity.profile.Exception.ProfileAlreadyExists;
import com.project.enginuity.profile.Exception.ProfileNotExists;
import com.project.enginuity.profile.Exception.UsernameAlreadyExists;
import com.project.enginuity.profile.io.ProfileRequest;
import com.project.enginuity.profile.io.ProfileResponse;
import com.project.enginuity.profile.io.ProfileReviewResponse;
import com.project.enginuity.profile.io.ProfileUpdateRequest;
import com.project.enginuity.profile.model.UserProfileEntity;
import com.project.enginuity.profile.repository.FollowRepo;
import com.project.enginuity.profile.repository.ProfileRepo;
import com.project.enginuity.profile.repository.UserRepo;
import com.project.enginuity.profile.service.ProfilePicService;
import com.project.enginuity.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    @Autowired
    private ProfileRepo profileRepo;
    @Autowired
    private ProfilePicService profilePicService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ReelRepo reelRepo;
    @Autowired
    private ReelServiceImpl reelService;

    private final RedisTemplate<String, String> redis;
    @Autowired
    private FollowRepo followRepo;

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
        List<String> interests= profile.getInterests();
        String key = "interest:"+userId;
        redis.opsForSet().add(key, interests.toArray(new String[0]));
        return convertToResponse(profile);
    }

    @Override
    public ProfileResponse getOtherProfile(String userName,String currentUserId,int page,int size) {
        UserProfileEntity profile = profileRepo.findByUserNameIgnoreCase(userName).orElseThrow(() -> new ProfileNotExists("Profile with this username does not exist!!"));
        String userId = profile.getUser().getUserId();
        UserProfileEntity myProfile = profileRepo.findByUser_UserId(currentUserId);
        boolean isMyProfile = profile.getUser().getUserId().equals(currentUserId);
        boolean isFollowed=false;
        if (!isMyProfile){
            isFollowed=followRepo.existsByFollowerAndFollowing(myProfile,profile);
        }
        Page<ReelEntity> reels = reelRepo.findByUser_UserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        ProfileResponse response = convertToResponse(profile);
        List<ReelResponse> reelResponses = reels.stream()
                .map(reel -> reelService.mapToReelResponse(reel))
                .toList();
        response.setReels(reelResponses);
        response.setMyProfile(isMyProfile);
        response.setFollowedByCurrentUser(isFollowed);
        response.setPage(reels.getNumber());
        response.setTotalPages(reels.getTotalPages());

        return response;

    }

    @Override
    public ProfileResponse getMyProfile(String userId, int page, int size) {
        UserProfileEntity profile = profileRepo.findByUser_UserId(userId);
        if (profile==null){
            throw new ProfileNotExists("Profile does not exist for this user!!");
        }
        Page<ReelEntity> reels = reelRepo.findByUser_UserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        ProfileResponse response = convertToResponse(profile);
        List<ReelResponse> reelResponses = reels.stream()
                .map(reel -> reelService.mapToReelResponse(reel))
                .toList();
        response.setReels(reelResponses);
        response.setMyProfile(true);
        response.setFollowedByCurrentUser(false);
        response.setPage(reels.getNumber());
        response.setTotalPages(reels.getTotalPages());
        return response;

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
