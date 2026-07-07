package com.example.kxbackend.domain.job.entity;

import com.example.kxbackend.domain.job.entity.enums.PromptKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "generate_prompt",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_generate_prompt_order",
                        columnNames = {"generate_job_id", "kind", "prompt_order"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GeneratePrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_job_id", nullable = false)
    private GenerateJob generateJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptKind kind;

    @Column(name = "prompt_order", nullable = false)
    private int promptOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

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
}
