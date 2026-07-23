package com.example.kxbackend.infra.ai.fal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.fal")
public class FalProperties {

    private String baseUrl;
    private String apiKey;
    private String webhookUrl;
}
