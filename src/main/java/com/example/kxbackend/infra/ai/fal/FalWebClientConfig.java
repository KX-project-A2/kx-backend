package com.example.kxbackend.infra.ai.fal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(FalProperties.class)
public class FalWebClientConfig {

    @Bean
    public WebClient falWebClient(FalProperties falProperties, WebClient.Builder webClientBuilder) {
        WebClient.Builder builder = webClientBuilder
                .baseUrl(falProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(falProperties.getApiKey())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Key " + falProperties.getApiKey());
        }

        return builder.build();
    }
}
