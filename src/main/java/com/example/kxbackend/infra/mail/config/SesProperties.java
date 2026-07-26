package com.example.kxbackend.infra.mail.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.ses")
public class SesProperties {

    /**
     * false면 실제 발송 대신 로그에 재설정 URL을 남긴다.
     */
    private boolean enabled = true;

    private String region = "ap-northeast-2";

    private String fromEmail = "";

    private String accessKey = "";

    private String secretKey = "";
}
