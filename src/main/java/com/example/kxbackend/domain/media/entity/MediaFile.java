package com.example.kxbackend.domain.media.entity;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.user.entity.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType type; // 'IMAGE' 또는 'VIDEO'

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(length = 200)
    private String model;

    @Column(length = 50)
    private String quality;

    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio;

    @Column(length = 50)
    private String resolution;

    @Column(name = "reversed_prompt", columnDefinition = "TEXT")
    private String reversedPrompt;

    @Column(length = 512)
    private String tags; // 검색 및 필터링용 태그 문자열

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_job_id")
    private GenerateJob generateJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_prompt_id")
    private GeneratePrompt generatePrompt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void connectGeneration(GenerateJob generateJob, GeneratePrompt generatePrompt) {
        this.generateJob = generateJob;
        this.generatePrompt = generatePrompt;
    }

    public void updateReversedPrompt(String reversedPrompt) {
        this.reversedPrompt = reversedPrompt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
