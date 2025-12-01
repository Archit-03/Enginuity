package com.project.enginuity.profile.io;

import com.project.enginuity.post.io.ReelResponse;
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
    private boolean isMyProfile;
    private boolean isFollowedByCurrentUser;
    private List<ReelResponse> reels;
    private int page;
    private int totalPages;
}
