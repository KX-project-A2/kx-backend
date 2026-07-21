package com.example.kxbackend.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 저장소 설정
 */
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * local | s3
     */
    private String type = "local";
    private final Local local = new Local();
    private final S3 s3 = new S3();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    public S3 getS3() {
        return s3;
    }

    public static class Local {
        private String basePath = "./storage";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }

    public static class S3 {
        private String bucket;
        private String region = "ap-northeast-2";
        private String accessKey;
        private String secretKey;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}
