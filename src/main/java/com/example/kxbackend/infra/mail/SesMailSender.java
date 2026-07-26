package com.example.kxbackend.infra.mail;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.mail.config.SesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Amazon SES 기반 메일 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SesMailSender {

    private final SesProperties sesProperties;
    private final ObjectProvider<SesClient> sesClientProvider;

    public void sendPasswordResetMail(String toEmail, String resetUrl) {
        String subject = "[KX] 비밀번호 재설정 안내";
        String textBody = """
                비밀번호 재설정을 요청하셨습니다.

                아래 링크에서 새 비밀번호를 설정해 주세요.
                %s

                링크는 일정 시간 후 만료되며, 본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(resetUrl);
        String htmlBody = """
                <p>비밀번호 재설정을 요청하셨습니다.</p>
                <p><a href="%s">비밀번호 재설정하기</a></p>
                <p>링크가 동작하지 않으면 아래 주소를 브라우저에 붙여넣어 주세요.</p>
                <p>%s</p>
                <p>링크는 일정 시간 후 만료되며, 본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
                """.formatted(resetUrl, resetUrl);

        if (!sesProperties.isEnabled()) {
            log.info("SES 비활성화 상태. 비밀번호 재설정 메일을 로그로 대체합니다. to={}, url={}", toEmail, resetUrl);
            return;
        }

        if (!StringUtils.hasText(sesProperties.getFromEmail())) {
            throw new BusinessException(
                    ErrorCode.EMAIL_SEND_FAILED,
                    "비밀번호 재설정 메일 발신 주소(AWS_SES_FROM_EMAIL)가 설정되지 않았습니다."
            );
        }

        SesClient sesClient = sesClientProvider.getIfAvailable();
        if (sesClient == null) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "Amazon SES 클라이언트를 사용할 수 없습니다.");
        }

        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .source(sesProperties.getFromEmail())
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().charset("UTF-8").data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().charset("UTF-8").data(textBody).build())
                                    .html(Content.builder().charset("UTF-8").data(htmlBody).build())
                                    .build())
                            .build())
                    .build());
            log.info("비밀번호 재설정 메일 발송 완료. to={}", toEmail);
        } catch (SesException exception) {
            log.error("Amazon SES 메일 발송 실패. to={}, awsError={}",
                    toEmail, exception.awsErrorDetails() != null ? exception.awsErrorDetails().errorMessage() : exception.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "비밀번호 재설정 메일 발송에 실패했습니다.");
        }
    }
}
