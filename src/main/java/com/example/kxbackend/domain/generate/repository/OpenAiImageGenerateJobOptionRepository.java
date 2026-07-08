package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenAiImageGenerateJobOptionRepository extends JpaRepository<OpenAiImageGenerateJobOption, Long> {
}
