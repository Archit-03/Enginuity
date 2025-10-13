package com.project.enginuity.post.repository;

import com.project.enginuity.post.entity.ReelFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReelFileRepo extends JpaRepository<ReelFileEntity, Long> {
    ReelFileEntity findByReelId(String reelId);

}
