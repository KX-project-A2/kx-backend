package com.example.kxbackend.domain.generate.entity;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이미지에서 추출한 역프롬프트 결과
 */
@Entity
@Table(name = "reverse_prompt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReversePrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 추출에 사용한 원본 미디어. 파일만 업로드한 경우 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_media_file_id")
    private MediaFile sourceMediaFile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * 다음에 생성할 때 사용할 목표 비율
     */
    @Column(name = "aspect_ratio", nullable = false, length = 20)
    private String aspectRatio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
