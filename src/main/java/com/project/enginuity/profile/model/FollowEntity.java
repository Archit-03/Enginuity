package com.project.enginuity.profile.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_follow")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FollowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id",nullable = false)
    private UserProfileEntity follower;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id",nullable = false)
    private UserProfileEntity following;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime followedAt;
}
