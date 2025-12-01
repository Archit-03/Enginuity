package com.project.enginuity.interaction.repository;

import com.project.enginuity.interaction.entities.ReelLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReelLiKeRepo extends JpaRepository<ReelLikeEntity,Long> {
    boolean existsByUser_UserIdAndReel_ReelId(String userId, String reelId);
    void deleteByUser_UserIdAndReel_ReelId(String userId, String reelId);
    long countByReel_ReelId(String reelId);
}
