package com.example.kxbackend.infra.mail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(SesProperties.class)
public class SesConfig {

    @Bean
    @ConditionalOnProperty(name = "aws.ses.enabled", havingValue = "true", matchIfMissing = true)
    public SesClient sesClient(SesProperties sesProperties) {
        log.info("Amazon SES 클라이언트 활성화. region={}, fromEmailConfigured={}",
                sesProperties.getRegion(),
                StringUtils.hasText(sesProperties.getFromEmail()));
        return SesClient.builder()
                .region(Region.of(sesProperties.getRegion()))
                .credentialsProvider(createCredentialsProvider(sesProperties))
                .build();
    }

    private AwsCredentialsProvider createCredentialsProvider(SesProperties sesProperties) {
        if (StringUtils.hasText(sesProperties.getAccessKey()) && StringUtils.hasText(sesProperties.getSecretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(sesProperties.getAccessKey(), sesProperties.getSecretKey())
            );
        }
        return DefaultCredentialsProvider.create();
    }
}
