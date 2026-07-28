package com.example.kxbackend.domain.media.entity;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.ReversePrompt;
import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GeneratedColumn;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "media_file",
        indexes = @Index(
                name = "uk_video_result_media_per_job",
                columnList = "video_result_generate_job_id",
                unique = true
        )
)
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

    @Column(length = 20)
    private String duration;

    /**
     * 이미지 생성 목적(캐릭터/배경). 업로드·영상 등은 null.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ImageGenerationPurpose purpose;

    /**
     * CHARACTER 다각도(정면/측면/후면) 배치 여부. 해당 없으면 null.
     */
    @Column(name = "multi_view_enabled")
    private Boolean multiViewEnabled;

    /**
     * 이 미디어에서 추출한 최신 역프롬프트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_prompt_id")
    private ReversePrompt reversedPrompt;

    @Column(length = 512)
    private String tags; // 검색 및 필터링용 태그 문자열

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_job_id")
    private GenerateJob generateJob;

    @GeneratedColumn("""
            case
                when type = 'VIDEO' and deleted = 0 and generate_job_id is not null
                then generate_job_id
                else null
            end
            """)
    @Column(name = "video_result_generate_job_id", insertable = false, updatable = false)
    private Long videoResultGenerateJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_prompt_id")
    private GeneratePrompt generatePrompt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void connectGeneration(GenerateJob generateJob, GeneratePrompt generatePrompt) {
        this.generateJob = generateJob;
        this.generatePrompt = generatePrompt;
    }

    public void linkReversedPrompt(ReversePrompt reversedPrompt) {
        this.reversedPrompt = reversedPrompt;
    }

    public void softDelete() {
        if (this.deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
