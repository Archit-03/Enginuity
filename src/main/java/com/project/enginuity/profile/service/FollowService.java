package com.project.enginuity.profile.service;

public interface FollowService {
    void followUser(String userId,String targetUsername);
    void unfollowUser(String userId,String targetUsername);
}
