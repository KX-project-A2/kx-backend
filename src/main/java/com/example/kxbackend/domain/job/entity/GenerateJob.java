package com.example.kxbackend.domain.job.entity;

import com.example.kxbackend.domain.media.entity.MediaFile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    @JoinColumn(name = "file_id")
    private MediaFile mediaFile;

    @Column(nullable = false, length = 20)
    private String type; // 'IMAGE', 'VIDEO', 'PROMPT'

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // 'PENDING', 'RUNNING', 'SUCCESS', 'FAILED'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
     * 비동기 작업 상태를 변경
     */
    public void updateStatus(String status) {
        this.status = status;
    }

    /**
     * 비동기 작업 성공 시 생성된 미디어 파일과 매핑
     */
    public void completeJob(MediaFile mediaFile) {
        this.status = "SUCCESS";
        this.mediaFile = mediaFile;
    }
}
