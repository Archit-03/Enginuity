package com.project.enginuity.profile.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VerifyOtpRequest {
    private String email;
    private String otp;
}
