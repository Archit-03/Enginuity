package com.project.enginuity.profile.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileUpdateRequest {
    private String username;
    @Size(max = 200 , message = "Bio should not exceed more than 200 words")
    private String bio;
    private List<String> skills;
    private List<String> interests;
    @URL(message = "Invalid url")
    private String githubUrl;
}
