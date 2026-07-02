package com.example.kxbackend.domain.media.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String type; // 'IMAGE' 또는 'VIDEO'

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "origin_prompt", nullable = false, columnDefinition = "TEXT")
    private String originPrompt;

    @Column(name = "reversed_prompt", columnDefinition = "TEXT")
    private String reversedPrompt;

    @Column(length = 512)
    private String tags; // 검색 및 필터링용 태그 문자열

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
