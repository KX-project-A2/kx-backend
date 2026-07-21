package com.example.kxbackend.infra.storage.s3;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * AWS S3 기반 객체 저장소
 */
@Slf4j
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        String key = ObjectStorageKeys.normalize(objectKey);
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (contentType != null && !contentType.isBlank()) {
                requestBuilder.contentType(contentType);
            }

            s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            String awsErrorCode = exception.awsErrorDetails() != null
                    ? exception.awsErrorDetails().errorCode()
                    : "UNKNOWN";
            log.error("S3 putObject 실패. bucket={}, key={}, statusCode={}, awsErrorCode={}, message={}",
                    bucket, key, exception.statusCode(), awsErrorCode, exception.getMessage());
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "S3 파일 저장에 실패했습니다. (" + awsErrorCode + ")"
            );
        } catch (SdkClientException exception) {
            log.error("S3 putObject 클라이언트 오류. bucket={}, key={}, message={}",
                    bucket, key, exception.getMessage());
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "S3 연결/인증에 실패했습니다. 자격 증명·리전·버킷을 확인하세요."
            );
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        String key = ObjectStorageKeys.normalize(objectKey);
        try {
            var response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );

            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new StoredObject(response.asByteArray(), contentType, ObjectStorageKeys.fileNameOf(key));
        } catch (NoSuchKeyException exception) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다.");
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다.");
            }
            log.error("S3 getObject 실패. bucket={}, key={}, statusCode={}, awsErrorCode={}, message={}",
                    bucket, key, exception.statusCode(), exception.awsErrorDetails().errorCode(), exception.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 조회에 실패했습니다.");
        } catch (SdkClientException exception) {
            log.error("S3 getObject 클라이언트 오류. bucket={}, key={}, message={}",
                    bucket, key, exception.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 연결/인증에 실패했습니다.");
        }
    }

    @Override
    public boolean exists(String objectKey) {
        String key = ObjectStorageKeys.normalize(objectKey);
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            log.error("S3 headObject 실패. bucket={}, key={}, statusCode={}, awsErrorCode={}, message={}",
                    bucket, key, exception.statusCode(), exception.awsErrorDetails().errorCode(), exception.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 존재 여부 확인에 실패했습니다.");
        }
    }
}
