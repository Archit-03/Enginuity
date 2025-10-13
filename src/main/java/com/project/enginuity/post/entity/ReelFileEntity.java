package com.project.enginuity.post.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reel_files")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ReelFileEntity {
    @Id
    private String reelId;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] reelData;
}
