package com.project.enginuity.post.entity;

import com.project.enginuity.post.io.ReelStatus;
import com.project.enginuity.profile.model.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

@Entity
@Table(name = "reels_tbl",
indexes = {
        @Index(name="idx_reel_status", columnList = "reelStatus"),
        @Index(name="idx_created_at", columnList = "createdAt"),
        @Index(name="idx_status_created_at", columnList = "reelStatus, createdAt DESC" )
}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)

public class ReelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String reelId;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId", nullable = false)
    private UserEntity user;
    private String reelUrl;
    private String description;
    @Enumerated(EnumType.STRING)
    private ReelStatus reelStatus;
    @ElementCollection
    @CollectionTable(name = "reel_tags", joinColumns = @JoinColumn(name = "reel_id"))
    @Column(name = "tag")
    private Set<String> tags;
    private int durationInSeconds;
    private String thumbnailUrl;
    private String summary;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

}
