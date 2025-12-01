package com.project.enginuity.interaction.repository;

import com.project.enginuity.interaction.entities.ReelCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReelCommentRepo extends JpaRepository<ReelCommentEntity,Long> {
    Page<ReelCommentEntity> findByReel_ReelIdOrderByCommentedAtDesc(String reelId, Pageable pageable);
    long countByReel_ReelId(String reelId);
}
