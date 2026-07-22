package com.example.kxbackend.domain.generate.entity;

import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OpenAI 이미지 생성 작업 옵션
 */
@Entity
@Table(name = "openai_image_generate_job_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OpenAiImageGenerateJobOption {

    @Id
    private Long generateJobId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generate_job_id")
    private GenerateJob generateJob;

    @Column(name = "image_count", nullable = false)
    private int imageCount;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(nullable = false, length = 20)
    private String quality;

    /**
     * 단일 목적(CHARACTER / BACKGROUND). 과거 데이터는 콤마 구분 문자열이 있을 수 있다.
     */
    @Column(name = "purposes", length = 50)
    private String purposes;

    public static OpenAiImageGenerateJobOption of(
            GenerateJob generateJob,
            int imageCount,
            String size,
            String quality,
            ImageGenerationPurpose purpose
    ) {
        return OpenAiImageGenerateJobOption.builder()
                .generateJob(generateJob)
                .imageCount(imageCount)
                .size(size)
                .quality(quality)
                .purposes(purpose.name())
                .build();
    }

    public ImageGenerationPurpose getPurpose() {
        if (purposes == null || purposes.isBlank()) {
            return ImageGenerationPurpose.CHARACTER;
        }
        String first = purposes.split(",")[0].trim();
        return ImageGenerationPurpose.valueOf(first);
    }
}
