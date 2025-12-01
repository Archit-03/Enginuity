package com.project.enginuity.profile.service;

import com.project.enginuity.profile.io.ResetPasswordRequest;
import com.project.enginuity.profile.io.UserRequest;
import com.project.enginuity.profile.io.UserResponse;
import com.project.enginuity.profile.io.VerifyOtpRequest;

public interface UserService {
    void registerUser(UserRequest userRequest);
    UserResponse verifyOtp(VerifyOtpRequest otpRequest);
    void resendOtp(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest resetRequest);
    UserResponse getUserByEmail(String email);


}
