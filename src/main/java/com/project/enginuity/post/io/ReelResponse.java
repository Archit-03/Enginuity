package com.project.enginuity.post.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
@AllArgsConstructor

public class ReelResponse {
    private String reelId;
    private String username;
    private String reelUrl;
    private String thumbnailUrl;
    private String description;
    private String createdAt;
}
