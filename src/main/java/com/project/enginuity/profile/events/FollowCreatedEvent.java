package com.project.enginuity.profile.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FollowCreatedEvent extends ApplicationEvent {
    private final String followerUsername;
    private final String followingUsername;
    public FollowCreatedEvent(Object source,String follower,String following) {
        super(source);
        this.followerUsername=follower;
        this.followingUsername=following;
    }
}
