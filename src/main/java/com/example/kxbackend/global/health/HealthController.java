package com.example.kxbackend.global.health;

import com.example.kxbackend.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success("OK", new HealthResponse("UP", Instant.now()));
    }

    public record HealthResponse(
            String status,
            Instant timestamp
    ) {
    }
}
