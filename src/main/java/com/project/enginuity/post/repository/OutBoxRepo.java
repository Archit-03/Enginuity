package com.project.enginuity.post.repository;

import com.project.enginuity.post.entity.OutboxEventEntity;
import com.project.enginuity.post.io.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutBoxRepo extends JpaRepository<OutboxEventEntity, Long> {
    List<OutboxEventEntity> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
