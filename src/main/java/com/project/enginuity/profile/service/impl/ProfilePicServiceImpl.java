package com.project.enginuity.profile.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.enginuity.profile.service.ProfilePicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfilePicServiceImpl implements ProfilePicService {
    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile profilePic) {
        String fileNameExtension = profilePic.getOriginalFilename().substring(profilePic.getOriginalFilename().lastIndexOf(".") + 1);
        String key = UUID.randomUUID().toString() + "." + fileNameExtension;
        Map<?, ?> uploadResult;
        try {
            Map<String, Object> uploadParam = ObjectUtils.asMap(
                    "file_name", key,
                    "overwrite", false,
                    "resource_type", "auto"
            );

            uploadResult = cloudinary.uploader().upload(profilePic.getBytes(), uploadParam);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (String) uploadResult.get("secure_url");
    }

    @Override
    public void deleteProfile(String imageUrl) throws IOException {
        cloudinary.uploader().destroy(imageUrl,ObjectUtils.emptyMap());
    }
}
