package com.project.enginuity.post.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReelDLQ {
    private String reelId;
    private String userId;
    private String reason;
    private Long timestamp;
}
