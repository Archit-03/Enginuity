package com.project.enginuity.profile.model;

import com.project.enginuity.profile.io.OtpType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Table(name = "tbl_otp_verification")
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class OtpVerificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String email;
    private String password;
    @Column(nullable = false)
    private String otp;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Enumerated(EnumType.STRING)
    private OtpType otpType;

}
