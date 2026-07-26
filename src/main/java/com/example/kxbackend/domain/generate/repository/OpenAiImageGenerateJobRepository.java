package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OpenAI 이미지 생성 작업 전용 조회 Repository
 */
public interface OpenAiImageGenerateJobRepository extends JpaRepository<GenerateJob, Long> {

    List<GenerateJob> findAllByStatusInAndType(List<Status> statuses, Type type);

    List<GenerateJob> findAllByUser_IdAndTypeAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Type type,
            List<Status> statuses
    );
}
