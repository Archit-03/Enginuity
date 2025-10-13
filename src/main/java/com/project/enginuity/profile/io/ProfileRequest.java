package com.project.enginuity.profile.io;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {
    @NotBlank(message = "Username should not be empty")
    private String username;
    @Size(max = 200 , message = "Bio should not exceed more than 200 words")
    private String bio;
    private String profilePictureUrl;
    private List<String> skills;
    private List<String> interests;
    @URL(message = "Invalid url")
    private String githubUrl;
}
