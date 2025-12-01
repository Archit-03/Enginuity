package com.project.enginuity.interaction.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private long id;
    private String userName;
    private String comment;
    private LocalDateTime commentedAt;
}
