package com.project.enginuity.profile.service.impl;

import com.project.enginuity.profile.Exception.AlreadyFollowingException;
import com.project.enginuity.profile.Exception.ProfileNotExists;
import com.project.enginuity.profile.events.FollowCreatedEvent;
import com.project.enginuity.profile.events.FollowRemovedEvent;
import com.project.enginuity.profile.model.FollowEntity;
import com.project.enginuity.profile.model.UserProfileEntity;
import com.project.enginuity.profile.repository.FollowRepo;
import com.project.enginuity.profile.repository.ProfileRepo;
import com.project.enginuity.profile.service.FollowService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    private FollowRepo followRepo;
    @Autowired
    private ProfileRepo profileRepo;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Override
    @Transactional
    public void followUser(String userId, String targetUsername) {
        UserProfileEntity follower=profileRepo.findByUser_UserId(userId);
        String currentUsername= follower.getUserName();
        UserProfileEntity following=profileRepo.findByUserNameIgnoreCase(targetUsername).orElseThrow(()->new ProfileNotExists("Target profile  not found!!"));
        if (followRepo.existsByFollowerAndFollowing(follower,following)){
            throw new AlreadyFollowingException("Already following!!");
        }
        FollowEntity follow=FollowEntity.builder()
                .follower(follower)
                .following(following)
                .followedAt(LocalDateTime.now())
                .build();

        followRepo.save(follow);
        applicationEventPublisher.publishEvent(new FollowCreatedEvent(this,currentUsername,targetUsername));

    }

    @Override
    @Transactional
    public void unfollowUser(String userId, String targetUsername) {
        UserProfileEntity follower=profileRepo.findByUser_UserId(userId);
        String currentUsername= follower.getUserName();
        UserProfileEntity following=profileRepo.findByUserNameIgnoreCase(targetUsername).orElseThrow(()->new ProfileNotExists("Target profile  not found!!"));
        if (!followRepo.existsByFollowerAndFollowing(follower,following)){
            throw new AlreadyFollowingException("Not following!!");
        }
        followRepo.deleteByFollowerAndFollowing(follower,following);

        applicationEventPublisher.publishEvent(new FollowRemovedEvent(this,currentUsername,targetUsername));

    }
}
