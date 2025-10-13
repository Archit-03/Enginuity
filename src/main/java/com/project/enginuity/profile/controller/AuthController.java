package com.project.enginuity.profile.controller;

import com.project.enginuity.profile.Exception.UserNotFoundException;
import com.project.enginuity.profile.io.AuthRequest;
import com.project.enginuity.profile.io.AuthResponse;
import com.project.enginuity.profile.io.UserResponse;
import com.project.enginuity.profile.security.JwtUtils;
import com.project.enginuity.profile.service.UserService;
import com.project.enginuity.profile.security.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            // Step 1: authenticate
            authenticate(authRequest.getEmail(), authRequest.getPassword());

            // Step 2: load user
            final UserDetails userDetails = userDetailService.loadUserByUsername(authRequest.getEmail());
            UserResponse userResponse = userService.getUserByEmail(authRequest.getEmail());
            if (userResponse == null) {
                throw new UserNotFoundException("User not found with email: " + authRequest.getEmail());
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
                    .body(new AuthResponse(authRequest.getEmail(), jwtToken));

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

    private void authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        ResponseCookie cookie=ResponseCookie.from("jwt","")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString())
                .body(Map.of("message","Logged out successfully!!"));
    }

}
