package com.project.enginuity.post.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalyzerRequest {
    private String reelId;
    private String description;
    private byte[] reelData;
}
