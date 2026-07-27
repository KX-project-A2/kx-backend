package com.example.kxbackend.domain.generate.entity;

import com.example.kxbackend.domain.generate.entity.enums.PromptKind;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generate_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GenerateJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type; // 'TEXT_TO_IMAGE', 'IMAGE_TO_VIDEO'

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // 'CREATED', 'SUBMITTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELED'

    @Column(name = "fal_request_id", unique = true, length = 100)
    private String falRequestId;

    @Column(name = "fal_model_id", length = 200)
    private String falModelId;

    @Column(name = "fal_status_url", length = 1000)
    private String falStatusUrl;

    @Column(name = "fal_response_url", length = 1000)
    private String falResponseUrl;

    @Column(name = "request_quality", length = 50)
    private String requestQuality;

    @Column(name = "request_aspect_ratio", length = 20)
    private String requestAspectRatio;

    @Column(name = "request_resolution", length = 50)
    private String requestResolution;

    @Column(name = "request_duration", length = 20)
    private String requestDuration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_media_file_id")
    private MediaFile inputMediaFile;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "generateJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("promptOrder ASC")
    @Builder.Default
    private List<GeneratePrompt> prompts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 생성 작업에 사용할 프롬프트를 추가
     */
    public void addPrompt(PromptKind kind, int promptOrder, String content) {
        GeneratePrompt prompt = GeneratePrompt.builder()
                .generateJob(this)
                .kind(kind)
                .promptOrder(promptOrder)
                .content(content)
                .build();
        this.prompts.add(prompt);
    }

    /**
     * fal.ai submit 요청 성공 시 외부 요청 ID를 기록
     */
    public void submit(String falRequestId) {
        this.falRequestId = falRequestId;
        this.status = Status.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * fal.ai submit 요청 성공 시 외부 요청 ID와 모델 ID를 기록
     */
    public void submit(String falRequestId, String falModelId) {
        this.falRequestId = falRequestId;
        this.falModelId = falModelId;
        this.status = Status.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * fal.ai submit 요청 성공 시 외부 요청 정보와 조회 URL을 기록
     */
    public void submit(String falRequestId, String falModelId, String falStatusUrl, String falResponseUrl) {
        this.falRequestId = falRequestId;
        this.falModelId = falModelId;
        this.falStatusUrl = falStatusUrl;
        this.falResponseUrl = falResponseUrl;
        this.status = Status.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * 외부 동기 API 요청 시작 상태를 기록한다.
     */
    public void startSynchronous() {
        this.status = Status.IN_PROGRESS;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * 외부 생성 작업 진행 상태로 변경
     */
    public void progress() {
        this.status = Status.IN_PROGRESS;
    }

    /**
     * 생성 작업 상태를 변경
     */
    public void updateStatus(Status status) {
        this.status = status;
    }

    /**
     * 생성 작업 성공 상태를 기록
     */
    public void completeJob() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 생성 작업 실패 상태와 원인을 기록
     */
    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 생성 작업 취소 상태로 변경
     */
    public void cancel() {
        this.status = Status.CANCELED;
        this.completedAt = LocalDateTime.now();
    }
}
