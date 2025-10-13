package com.project.enginuity.post.repository;

import com.project.enginuity.post.entity.ReelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReelRepo extends JpaRepository<ReelEntity , Long> {
    ReelEntity findByReelId(String reelId);
}
