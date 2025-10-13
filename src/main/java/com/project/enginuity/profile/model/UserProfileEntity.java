package com.project.enginuity.profile.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_profile",
        indexes = {
                @Index(name = "idx_username", columnList = "userName")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true, nullable = false)
    private String profileId;
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId", nullable = false)
    private UserEntity user;
    @Column(unique = true, nullable = false)
    private String userName;
    private String bio;
    @ElementCollection
    private List<String> skills=new ArrayList<>();
    private String profilePictureUrl;
    @ElementCollection
    private List<String> interests=new ArrayList<>();
    @URL
    private String githubUrl;
    private int followerCount=0;
    private int followingCount=0;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    @Version
    private Long version;
}
