package com.project.enginuity.profile.repository;

import com.project.enginuity.profile.model.FollowEntity;
import com.project.enginuity.profile.model.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepo extends JpaRepository<FollowEntity,Long> {
    boolean existsByFollowerAndFollowing(UserProfileEntity follower,UserProfileEntity following);
    void deleteByFollowerAndFollowing(UserProfileEntity follower,UserProfileEntity following);

}
