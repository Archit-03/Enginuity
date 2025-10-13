package com.project.enginuity.post.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalyzerResponse {
    private String text;
    private String summary;
    private List<String> tags;
    private boolean approved;
}
