package com.project.enginuity.profile.service;

import com.project.enginuity.profile.io.ProfileRequest;
import com.project.enginuity.profile.io.ProfileResponse;
import com.project.enginuity.profile.io.ProfileReviewResponse;
import com.project.enginuity.profile.io.ProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest profileRequest, MultipartFile profileUrl,String userId);
    ProfileResponse getProfile(String userId);
    ProfileResponse editProfile(ProfileUpdateRequest profileUpdateRequest,String userId);
    void editProfilePic(MultipartFile updatedProfile,String userId);
    void deleteProfile(String userId);
    List<ProfileReviewResponse> searchProfilesByUsername(String username);
    ProfileResponse getProfileByUsername(String username);
    boolean checkUsernameAvailability(String username);
    void deleteProfilePic(String userId);

 }
