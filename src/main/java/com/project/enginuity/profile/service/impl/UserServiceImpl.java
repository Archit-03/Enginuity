package com.project.enginuity.profile.service.impl;

import com.project.enginuity.profile.model.OtpVerificationEntity;
import com.project.enginuity.profile.model.UserEntity;
import com.project.enginuity.profile.Exception.*;
import com.project.enginuity.profile.io.*;
import com.project.enginuity.profile.repository.OtpVerificationRepo;
import com.project.enginuity.profile.repository.UserRepo;
import com.project.enginuity.profile.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private OtpVerificationRepo otpVerificationRepo;
    @Autowired
    private EmailServiceImpl emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailServiceImpl emailServiceImpl;


    @Override
    public void registerUser(UserRequest userRequest) {

        if (userRepo.existsByEmail(userRequest.getEmail())){
            throw new EmailAlreadyExists("User with this email already exists!!");
        }
        otpVerificationRepo.deleteByEmail(userRequest.getEmail());
        String otp=generateOtp();
        OtpVerificationEntity otpVerification=OtpVerificationEntity.builder()
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .otp(otp)
                .otpType(OtpType.REGISTER)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpVerificationRepo.save(otpVerification);
        emailService.sendOtpEmail(userRequest.getEmail(),otp);
    }

    @Override
    public UserResponse verifyOtp(VerifyOtpRequest otpRequest) {
        OtpVerificationEntity otpEntity=otpVerificationRepo.findByEmail(otpRequest.getEmail())
                .orElseThrow(()->new OtpNotFoundException("Otp with this email not found"));
        if (!otpEntity.getOtp().equals(otpRequest.getOtp())){
            throw new InvalidOtp("Invalid Otp!!");
        }

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new OtpExpired("Otp Expired...Resend Otp!!");
        }

        if(otpEntity.getOtpType()!=OtpType.REGISTER){
            throw new InvalidOtp("OTP type mismatch");
        }

        UserEntity user=UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .email(otpEntity.getEmail())
                .password(otpEntity.getPassword())
                .isAccountVerified(true)
                .build();
        userRepo.save(user);
        otpVerificationRepo.delete(otpEntity);
        return convertToResponse(user);
    }

    @Override
    public void resendOtp(String email) {
        OtpVerificationEntity otpEntity=otpVerificationRepo.findByEmail(email)
                .orElseThrow(()->new OtpNotFoundException("No OTP request found!!"));
        String otp=generateOtp();
        otpEntity.setOtp(otp);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpVerificationRepo.save(otpEntity);

        emailService.sendOtpEmail(email,otp);
    }

    @Override
    public void forgotPassword(String email) {
        UserEntity user=userRepo.findByEmail(email).orElseThrow(()->new UserNotFoundException("User not found!!"));
        String otp=generateOtp();
        OtpVerificationEntity otpEntity=OtpVerificationEntity.builder()
                .email(user.getEmail())
                .otp(otp)
                .otpType(OtpType.RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpVerificationRepo.save(otpEntity);
        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Override
    public void resetPassword(ResetPasswordRequest resetRequest) {
        OtpVerificationEntity otpEntity=otpVerificationRepo.findByEmail(resetRequest.getEmail()).
                orElseThrow(()->new OtpNotFoundException("No OTP request found!!"));

        if (!otpEntity.getOtp().equals(resetRequest.getOtp()) || otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtp("Invalid or expired OTP");
        }

        UserEntity user=userRepo.findByEmail(resetRequest.getEmail())
                .orElseThrow(()->new UserNotFoundException("User not found!!"));
        user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
        userRepo.save(user);
        otpVerificationRepo.deleteByEmail(resetRequest.getEmail());
    }

    @Scheduled(fixedRate = 3600000)
    private void deleteOtp(){
        otpVerificationRepo.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    public String generateOtp(){
        return String.format("%06d",new SecureRandom().nextInt(1_000_000));
    }


    @Override
    public UserResponse getUserByEmail(String email) {
        return convertToResponse(userRepo.findByEmail(email).orElseThrow(()->new UserNotFoundException("User not found!!")));

    }


    private UserResponse convertToResponse(UserEntity user){
        return UserResponse.builder()
                .userID(user.getUserId())
                .email(user.getEmail())
                .isAccountVerified(user.getIsAccountVerified())
                .build();
    }
}
