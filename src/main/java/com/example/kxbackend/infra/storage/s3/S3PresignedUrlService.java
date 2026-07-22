package com.example.kxbackend.infra.storage.s3;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private static final long DEFAULT_EXPIRATION_SECONDS = 300;

    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final StorageProperties storageProperties;

    public PresignedUrl createDownloadUrl(String objectKey, String fileName) {
        return createPresignedGetUrl(
                objectKey,
                buildAttachmentContentDisposition(resolveFileName(fileName, objectKey))
        );
    }

    public PresignedUrl createReadUrl(String objectKey) {
        return createPresignedGetUrl(objectKey, null);
    }

    private PresignedUrl createPresignedGetUrl(String objectKey, String contentDisposition) {
        S3Presigner s3Presigner = getS3Presigner();
        StorageProperties.S3 s3 = storageProperties.getS3();
        String bucket = s3.getBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 버킷 설정이 필요합니다.");
        }

        // DB에 저장된 filePath를 S3 object key 형태로 정규화
        String key = ObjectStorageKeys.normalize(objectKey);
        // 설정값이 비정상이면 기본 만료 시간으로 보정
        long expiresInSeconds = resolveExpirationSeconds(s3.getPresignedUrlExpirationSeconds());
        // S3 GET 요청에 버킷과 object key 설정
        GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (StringUtils.hasText(contentDisposition)) {
            // 다운로드용 URL에는 attachment 응답 헤더 설정
            getObjectRequestBuilder.responseContentDisposition(contentDisposition);
        }
        GetObjectRequest getObjectRequest = getObjectRequestBuilder.build();

        // presigned URL 서명 유효 시간 설정 후 생성
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        return new PresignedUrl(
                s3Presigner.presignGetObject(presignRequest).url().toString(),
                expiresInSeconds
        );
    }


    /**
     * helper method
     */

    private S3Presigner getS3Presigner() {
        // storage.type=s3가 아닐 때는 S3Presigner 빈이 없으므로 호출 시점에 차단
        S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
        if (s3Presigner == null) {
            throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "S3 presigned URL 설정이 필요합니다.");
        }
        return s3Presigner;
    }

    private long resolveExpirationSeconds(long configuredSeconds) {
        // 0 이하 설정값은 사용할 수 없으므로 기본값 적용
        if (configuredSeconds <= 0) {
            return DEFAULT_EXPIRATION_SECONDS;
        }
        return configuredSeconds;
    }

    private String resolveFileName(String fileName, String objectKey) {
        // 호출자가 파일명을 넘기면 우선 사용
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        // 파일명이 없으면 object key 마지막 경로를 파일명으로 사용
        return ObjectStorageKeys.fileNameOf(objectKey);
    }

    private String buildAttachmentContentDisposition(String fileName) {
        // 브라우저가 열기 대신 다운로드로 처리하도록 attachment 헤더 설정
        return "attachment; filename=\"" + fileName.replace("\"", "") + "\"";
    }

    public record PresignedUrl(String url, long expiresInSeconds) {
    }
}
