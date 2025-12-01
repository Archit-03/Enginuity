package com.project.enginuity.post.repository;

import com.project.enginuity.post.entity.ReelFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReelFileRepo extends JpaRepository<ReelFileEntity, Long> {
    ReelFileEntity findByReelId(String reelId);
    boolean existsByReelId(String reelId);

}
