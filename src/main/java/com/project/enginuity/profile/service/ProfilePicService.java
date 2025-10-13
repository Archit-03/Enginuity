package com.project.enginuity.profile.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProfilePicService {
    String uploadImage(MultipartFile profilePic);
    void deleteProfile(String imageUrl) throws IOException;
}
