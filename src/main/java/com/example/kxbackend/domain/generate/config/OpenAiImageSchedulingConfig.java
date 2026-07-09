package com.example.kxbackend.domain.generate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpenAI 이미지 생성 배치 상태 폴링 스케줄러 활성화
 */
@Configuration
@EnableScheduling
public class OpenAiImageSchedulingConfig {
}
