package com.project.enginuity.profile.repository;
import com.project.enginuity.profile.model.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepo extends JpaRepository<UserProfileEntity,Long>{
    boolean existsByUserNameIgnoreCase(String username);
    Optional<UserProfileEntity> findByUserNameIgnoreCase(String username);
    List<UserProfileEntity> findTop5ByUserNameContainingIgnoreCaseOrderByUserNameAsc(String username);
    UserProfileEntity findByUser_UserId(String userId);
    boolean existsByUser_UserId(String userId);
}

