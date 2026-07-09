package com.example.kxbackend.domain.generate.client;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationCommand;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationSubmitResult;

public interface VideoGenerationClient {

    VideoGenerationSubmitResult submit(VideoGenerationCommand command);

    VideoGenerationStatusResult getStatus(String modelId, String requestId, boolean withLogs);

    VideoGenerationStatusResult getStatusByUrl(String statusUrl, boolean withLogs);

    VideoGenerationResult getResult(String modelId, String requestId);

    VideoGenerationResult getResultByUrl(String responseUrl);
}
