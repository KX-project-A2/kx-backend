package com.example.kxbackend.domain.generate.entity;

import com.example.kxbackend.domain.generate.entity.enums.ReferenceImageType;
import com.example.kxbackend.domain.media.entity.MediaFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OpenAI 이미지 생성 작업에 사용된 레퍼런스 이미지
 */
@Entity
@Table(name = "openai_image_reference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OpenAiImageReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generate_job_id", nullable = false)
    private GenerateJob generateJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private ReferenceImageType referenceType;

    @Column(name = "reference_order", nullable = false)
    private int referenceOrder;

    @Column(name = "openai_file_id", nullable = false, length = 100)
    private String openAiFileId;
}
