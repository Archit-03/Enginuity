package com.project.enginuity.interaction.repository;

import com.project.enginuity.interaction.entities.ReelSaveEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReelSaveRepo extends JpaRepository<ReelSaveEntity,Long> {
    boolean existsByUser_UserIdAndReel_ReelId(String userId,String reelId);
    void  deleteByUser_UserIdAndReel_ReelId(String userId,String reelId);
    Page<ReelSaveEntity> findByUser_UserIdOrderBySavedAtDesc(String userId, Pageable pageable);
}
