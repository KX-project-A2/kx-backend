package com.example.kxbackend.domain.share.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "share.link")
public class ShareLinkProperties {

    private static final int MIN_TOKEN_BYTE_LENGTH = 16;
    private static final int MAX_TOKEN_BYTE_LENGTH = 96;

    private int expirationDays = 7;
    private int tokenByteLength = 32;
    private int maxTokenGenerationAttempts = 5;

    public int getExpirationDays() {
        return Math.max(expirationDays, 1);
    }

    public void setExpirationDays(int expirationDays) {
        this.expirationDays = expirationDays;
    }

    public int getTokenByteLength() {
        return Math.min(Math.max(tokenByteLength, MIN_TOKEN_BYTE_LENGTH), MAX_TOKEN_BYTE_LENGTH);
    }

    public void setTokenByteLength(int tokenByteLength) {
        this.tokenByteLength = tokenByteLength;
    }

    public int getMaxTokenGenerationAttempts() {
        return Math.max(maxTokenGenerationAttempts, 1);
    }

    public void setMaxTokenGenerationAttempts(int maxTokenGenerationAttempts) {
        this.maxTokenGenerationAttempts = maxTokenGenerationAttempts;
    }
}
