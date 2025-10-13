package com.project.enginuity.profile.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private String username;
    private String bio;
    private String profilePictureUrl;
    private List<String> skills;
    private List<String> interests;
    private String githubUrl;
    private int followerCount;


}
