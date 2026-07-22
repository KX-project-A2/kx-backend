package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpenAiImageReferenceRepository extends JpaRepository<OpenAiImageReference, Long> {

    List<OpenAiImageReference> findAllByGenerateJob_IdOrderByReferenceOrderAsc(Long generateJobId);
}
