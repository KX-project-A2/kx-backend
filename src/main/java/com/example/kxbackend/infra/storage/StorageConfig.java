package com.example.kxbackend.infra.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 저장소(Local / S3) 빈 설정
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public ObjectStorage localObjectStorage(StorageProperties storageProperties) {
        return new LocalObjectStorage(storageProperties.getLocal().getBasePath());
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public S3Client s3Client(StorageProperties storageProperties) {
        StorageProperties.S3 s3 = storageProperties.getS3();
        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()));

        if (StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey())) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())
                    )
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public ObjectStorage s3ObjectStorage(S3Client s3Client, StorageProperties storageProperties) {
        StorageProperties.S3 s3 = storageProperties.getS3();
        String bucket = s3.getBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("storage.type=s3 일 때 storage.s3.bucket 설정이 필요합니다.");
        }
        log.info("S3 저장소 활성화. bucket={}, region={}, accessKeyConfigured={}",
                bucket, s3.getRegion(), StringUtils.hasText(s3.getAccessKey()));
        return new S3ObjectStorage(s3Client, bucket);
    }
}
