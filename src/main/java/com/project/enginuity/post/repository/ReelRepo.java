package com.project.enginuity.post.repository;

import com.project.enginuity.post.entity.ReelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReelRepo extends JpaRepository<ReelEntity , Long> {
    ReelEntity findByReelId(String reelId);
    Page<ReelEntity> findByUser_UserId(String userId, Pageable pageable);

    List<ReelEntity> findAllByReelIdIn(List<String> orderedIds);
}
