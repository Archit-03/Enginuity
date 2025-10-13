package com.project.enginuity.profile.events;

import com.project.enginuity.profile.model.UserProfileEntity;
import com.project.enginuity.profile.repository.ProfileRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class FollowEventListener {
    @Autowired
    private ProfileRepo profileRepo;
    @EventListener
    @Async
    @Transactional
    public void handleFollowCreated(FollowCreatedEvent createdEvent){
        UserProfileEntity follower = profileRepo.findByUserNameIgnoreCase(createdEvent.getFollowerUsername())
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        UserProfileEntity following = profileRepo.findByUserNameIgnoreCase(createdEvent.getFollowingUsername())
                .orElseThrow(() -> new RuntimeException("Following not found"));

        follower.setFollowingCount(follower.getFollowingCount() + 1);
        following.setFollowerCount(following.getFollowerCount() + 1);
        profileRepo.save(follower);
        profileRepo.save(following);
    }
    @EventListener
    @Async
    @Transactional
    public void handleFollowRemoved(FollowRemovedEvent removedEvent){
        UserProfileEntity follower = profileRepo.findByUserNameIgnoreCase(removedEvent.getFollowerUsername())
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        UserProfileEntity following = profileRepo.findByUserNameIgnoreCase(removedEvent.getFollowingUsername())
                .orElseThrow(() -> new RuntimeException("Following not found"));

        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
        following.setFollowerCount(Math.max(0, following.getFollowerCount() - 1));
        profileRepo.save(follower);
        profileRepo.save(following);
    }


}
