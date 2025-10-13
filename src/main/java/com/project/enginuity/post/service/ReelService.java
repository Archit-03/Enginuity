package com.project.enginuity.post.service;

import com.project.enginuity.post.io.ReelRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ReelService {
    void uploadReel(String userId, ReelRequest reelRequest, MultipartFile file) throws IOException;

}
