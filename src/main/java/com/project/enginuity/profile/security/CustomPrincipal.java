package com.project.enginuity.profile.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomPrincipal {
    private String userId;
    private String email;
}
