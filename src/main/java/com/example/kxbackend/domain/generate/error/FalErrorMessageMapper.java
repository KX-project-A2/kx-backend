package com.example.kxbackend.domain.generate.error;

import org.springframework.util.StringUtils;

public final class FalErrorMessageMapper {

    private FalErrorMessageMapper() {
    }

    public static String toUserMessage(String type, String fallbackMessage, String statusCode) {
        if (!StringUtils.hasText(type)) {
            return format(null, fallbackMessage, statusCode);
        }

        String message = switch (type) {
            case "content_policy_violation" ->
                    "입력한 내용이 안전 정책에 의해 처리되지 않았습니다. 프롬프트나 이미지를 수정한 뒤 다시 시도해 주세요.";
            case "no_media_generated" ->
                    "입력한 내용으로 결과물을 생성하지 못했습니다. 프롬프트나 이미지를 변경한 뒤 다시 시도해 주세요.";
            case "image_too_small" ->
                    "이미지 해상도가 너무 낮습니다. 더 큰 이미지를 사용해 주세요.";
            case "image_too_large" ->
                    "이미지 해상도가 너무 큽니다. 더 작은 이미지를 사용해 주세요.";
            case "image_load_error" ->
                    "이미지를 불러오거나 처리할 수 없습니다. 손상되지 않은 PNG, JPG, WEBP 이미지를 사용해 주세요.";
            case "file_download_error" ->
                    "입력 파일을 다운로드할 수 없습니다. 파일 접근 가능 여부를 확인한 뒤 다시 시도해 주세요.";
            case "file_too_large" ->
                    "파일 크기가 너무 큽니다. 더 작은 파일을 사용해 주세요.";
            case "face_detection_error" ->
                    "이미지에서 얼굴을 감지하지 못했습니다. 얼굴이 선명하게 보이는 이미지를 사용해 주세요.";
            case "generation_timeout" ->
                    "생성 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            case "downstream_service_error" ->
                    "외부 생성 서비스 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            case "downstream_service_unavailable" ->
                    "외부 생성 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
            case "internal_server_error" ->
                    "생성 서버에서 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            case "greater_than", "greater_than_equal", "less_than", "less_than_equal", "multiple_of", "one_of" ->
                    "입력 옵션 값이 허용 범위를 벗어났습니다. 옵션 값을 확인한 뒤 다시 시도해 주세요.";
            case "sequence_too_short" ->
                    "입력 항목 수가 부족합니다. 필요한 값을 추가한 뒤 다시 시도해 주세요.";
            case "sequence_too_long" ->
                    "입력 항목 수가 너무 많습니다. 일부 항목을 줄인 뒤 다시 시도해 주세요.";
            case "feature_not_supported" ->
                    "선택한 모델에서 지원하지 않는 기능입니다. 다른 옵션이나 모델을 선택해 주세요.";
            case "invalid_archive" ->
                    "압축 파일을 처리할 수 없습니다. 파일 형식과 내부 파일을 확인해 주세요.";
            case "archive_file_count_below_minimum" ->
                    "압축 파일 안의 파일 수가 부족합니다. 필요한 파일을 추가해 주세요.";
            case "archive_file_count_exceeds_maximum" ->
                    "압축 파일 안의 파일 수가 너무 많습니다. 일부 파일을 줄여 주세요.";
            case "audio_duration_too_long" ->
                    "오디오 길이가 너무 깁니다. 더 짧은 오디오를 사용해 주세요.";
            case "audio_duration_too_short" ->
                    "오디오 길이가 너무 짧습니다. 더 긴 오디오를 사용해 주세요.";
            case "unsupported_audio_format" ->
                    "지원하지 않는 오디오 형식입니다. 지원되는 형식의 오디오를 사용해 주세요.";
            case "unsupported_image_format" ->
                    "지원하지 않는 이미지 형식입니다. PNG, JPG, WEBP 이미지를 사용해 주세요.";
            case "unsupported_video_format" ->
                    "지원하지 않는 영상 형식입니다. 지원되는 형식의 영상을 사용해 주세요.";
            case "video_duration_too_long" ->
                    "영상 길이가 너무 깁니다. 더 짧은 영상을 사용해 주세요.";
            case "video_duration_too_short" ->
                    "영상 길이가 너무 짧습니다. 더 긴 영상을 사용해 주세요.";
            case "request_timeout" ->
                    "요청 처리 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            case "startup_timeout" ->
                    "생성 작업 준비 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_scheduling_failure" ->
                    "생성 작업을 배정하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_connection_timeout" ->
                    "생성 서버 연결 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_disconnected" ->
                    "생성 서버 연결이 중간에 끊겼습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_connection_refused" ->
                    "생성 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_connection_error" ->
                    "생성 서버 연결 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_incomplete_response" ->
                    "생성 서버 응답이 완전하지 않습니다. 잠시 후 다시 시도해 주세요.";
            case "runner_server_error" ->
                    "생성 서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            case "client_disconnected" ->
                    "요청 연결이 중간에 끊겼습니다. 네트워크 상태를 확인한 뒤 다시 시도해 주세요.";
            case "client_cancelled" ->
                    "요청이 취소되었습니다. 다시 생성해 주세요.";
            case "bad_request" ->
                    "요청 형식이 올바르지 않습니다. 입력값과 옵션을 확인해 주세요.";
            case "internal_error" ->
                    "생성 요청 처리 중 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            default -> fallbackMessage;
        };
        return format(type, message, statusCode);
    }

    public static String format(String type, String message, String statusCode) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(type)) {
            builder.append('[').append(type).append("] : ");
        }
        builder.append(message);
        if (StringUtils.hasText(statusCode)) {
            builder.append(" (HTTP ").append(statusCode).append(')');
        }
        return builder.toString();
    }
}
