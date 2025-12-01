package com.project.enginuity.profile.controller;

import com.project.enginuity.profile.Exception.UserNotFoundException;
import com.project.enginuity.profile.io.*;
import com.project.enginuity.profile.security.JwtUtils;
import com.project.enginuity.profile.security.UserDetailService;
import com.project.enginuity.profile.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private JwtUtils jwtUtils;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequest userRequest){
        userService.registerUser(userRequest);
        return ResponseEntity.ok("Otp is sent to your email for registration !!");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest otpRequest){
        try {
            UserResponse userResponse=userService.verifyOtp(otpRequest);

            // Step 2: load user
            final UserDetails userDetails = userDetailService.loadUserByUsername(otpRequest.getEmail());
            if (userResponse == null) {
                throw new UserNotFoundException("User not found with email: " + otpRequest.getEmail());
            }

            // Step 3: generate token
            String jwtToken = jwtUtils.generateToken(userDetails, userResponse.getUserID());

            // Step 4: set cookie
            ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                    .httpOnly(true)
                    .secure(false) // enable only if https
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .sameSite("Strict")
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(new AuthResponse(otpRequest.getEmail(), jwtToken));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", true,
                    "message", "Invalid email or password"
            ));
        } catch (DisabledException ex) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", true,
                    "message", "Account is disabled, please contact support"
            ));
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", true,
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", true,
                    "message", "Authentication failed due to an unexpected error: " + ex.getMessage()
            ));
        }
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest otpRequest){
        userService.resendOtp(otpRequest.getEmail());
        return ResponseEntity.ok("Otp resent successfully!!");
    }
    @PostMapping("/send-reset-otp")
    public ResponseEntity<?> forgotPasswordOtp(@RequestBody ResendOtpRequest otpRequest){
        userService.forgotPassword(otpRequest.getEmail());
        return ResponseEntity.ok("OTP sent to email for reset password!!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request){
        userService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully!!");
    }


}
