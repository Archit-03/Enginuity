package com.project.enginuity.profile.repository;

import com.project.enginuity.profile.model.OtpVerificationEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
@Repository
public interface OtpVerificationRepo extends JpaRepository<OtpVerificationEntity,Long> {
    Optional<OtpVerificationEntity> findByEmail(String email);
    @Modifying
    @Transactional
    void deleteByEmail( String email);

    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
