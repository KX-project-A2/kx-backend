package com.example.kxbackend.domain.generate.validation;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class VideoOptionValidator {

    public static final String KLING_O3_STANDARD_REFERENCE_TO_VIDEO_MODEL_ID =
            "fal-ai/kling-video/o3/standard/reference-to-video";
    public static final String SEEDANCE_REFERENCE_TO_VIDEO_MODEL_ID =
            "bytedance/seedance-2.0/reference-to-video";

    private static final Set<String> SUPPORTED_VIDEO_MODEL_IDS = Set.of(
            KLING_O3_STANDARD_REFERENCE_TO_VIDEO_MODEL_ID,
            SEEDANCE_REFERENCE_TO_VIDEO_MODEL_ID
    );
    private static final Set<String> KLING_DURATION_VALUES = Set.of(
            "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
    );
    private static final Set<String> SEEDANCE_DURATION_VALUES = Set.of(
            "auto", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
    );
    private static final Set<String> KLING_REFERENCE_ASPECT_RATIO_VALUES = Set.of("16:9", "9:16", "1:1");
    private static final Set<String> SEEDANCE_ASPECT_RATIO_VALUES = Set.of(
            "auto", "21:9", "16:9", "4:3", "1:1", "3:4", "9:16"
    );
    private static final Set<String> SEEDANCE_RESOLUTION_VALUES = Set.of("480p", "720p", "1080p", "4k");
    private static final Set<String> SHOT_TYPE_VALUES = Set.of("customize", "intelligent");
    private static final Set<String> SEEDANCE_BITRATE_MODE_VALUES = Set.of("standard", "high");
    private static final Set<String> KLING_REFERENCE_TO_VIDEO_OPTION_KEYS = Set.of(
            "duration", "generate_audio", "multi_prompt", "shot_type", "aspect_ratio", "elements"
    );
    private static final Set<String> SEEDANCE_REFERENCE_TO_VIDEO_OPTION_KEYS = Set.of(
            "resolution", "duration", "aspect_ratio", "generate_audio", "bitrate_mode", "end_user_id"
    );

    public String resolveModelId(String modelId) {
        String resolvedModelId = modelId == null || modelId.isBlank()
                ? KLING_O3_STANDARD_REFERENCE_TO_VIDEO_MODEL_ID
                : modelId;
        if (!SUPPORTED_VIDEO_MODEL_IDS.contains(resolvedModelId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 영상 생성 모델입니다.");
        }
        return resolvedModelId;
    }

    public void validate(String modelId, String prompt, Map<String, Object> options) {
        validatePromptInput(prompt, options);

        Set<String> allowedKeys = allowedVideoOptionKeys(modelId);
        if (options == null || options.isEmpty()) {
            return;
        }
        for (String key : options.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 영상 생성 옵션입니다: " + key);
            }
        }

        validateDuration(modelId, options.get("duration"));
        validateGenerateAudio(options.get("generate_audio"));
        validateMultiPrompt(options.get("multi_prompt"));
        validateElements(options.get("elements"));
        validateShotType(options.get("shot_type"));
        validateAspectRatio(modelId, options.get("aspect_ratio"));
        validateResolution(modelId, options.get("resolution"));
        validateBitrateMode(options.get("bitrate_mode"));
        validateEndUserId(options.get("end_user_id"));
    }

    public boolean isSeedanceReferenceToVideoModel(String modelId) {
        return SEEDANCE_REFERENCE_TO_VIDEO_MODEL_ID.equals(modelId);
    }

    public boolean isReferenceToVideoModel(String modelId) {
        return KLING_O3_STANDARD_REFERENCE_TO_VIDEO_MODEL_ID.equals(modelId);
    }

    private Set<String> allowedVideoOptionKeys(String modelId) {
        if (KLING_O3_STANDARD_REFERENCE_TO_VIDEO_MODEL_ID.equals(modelId)) {
            return KLING_REFERENCE_TO_VIDEO_OPTION_KEYS;
        }
        return SEEDANCE_REFERENCE_TO_VIDEO_OPTION_KEYS;
    }

    private void validatePromptInput(String prompt, Map<String, Object> options) {
        boolean hasPrompt = prompt != null && !prompt.isBlank();
        boolean hasMultiPrompt = hasMultiPrompt(options);
        if (!hasPrompt && !hasMultiPrompt) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "프롬프트 또는 multi_prompt가 필요합니다.");
        }
        if (hasPrompt && hasMultiPrompt) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "prompt와 multi_prompt는 동시에 사용할 수 없습니다.");
        }
    }

    private void validateDuration(String modelId, Object value) {
        if (value == null) {
            return;
        }
        Set<String> allowedValues = isSeedanceReferenceToVideoModel(modelId)
                ? SEEDANCE_DURATION_VALUES
                : KLING_DURATION_VALUES;
        validateEnumValue("duration", value, allowedValues);
    }

    private void validateGenerateAudio(Object value) {
        if (value == null || value instanceof Boolean) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "generate_audio는 boolean 값이어야 합니다.");
    }

    private void validateMultiPrompt(Object value) {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> multiPrompt)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt는 배열이어야 합니다.");
        }
        if (multiPrompt.isEmpty() || multiPrompt.size() > 6) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt는 1개 이상 6개 이하의 샷이어야 합니다.");
        }

        int totalDuration = 0;
        for (Object item : multiPrompt) {
            if (!(item instanceof Map<?, ?> shot)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt 각 항목은 객체여야 합니다.");
            }
            Object prompt = shot.get("prompt");
            if (!(prompt instanceof String promptValue) || promptValue.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt 각 항목에는 prompt가 필요합니다.");
            }
            Object duration = shot.get("duration");
            int durationValue = parseMultiPromptDuration(duration);
            totalDuration += durationValue;
        }
        if (totalDuration > 15) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt 전체 duration은 15초 이하여야 합니다.");
        }
    }

    private void validateElements(Object value) {
        if (value == null || value instanceof List<?>) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "elements는 배열이어야 합니다.");
    }

    private void validateShotType(Object value) {
        if (value == null) {
            return;
        }
        validateEnumValue("shot_type", value, SHOT_TYPE_VALUES);
    }

    private void validateAspectRatio(String modelId, Object value) {
        if (value == null) {
            return;
        }
        Set<String> allowedValues = isSeedanceReferenceToVideoModel(modelId)
                ? SEEDANCE_ASPECT_RATIO_VALUES
                : KLING_REFERENCE_ASPECT_RATIO_VALUES;
        validateEnumValue("aspect_ratio", value, allowedValues);
    }

    private void validateResolution(String modelId, Object value) {
        if (value == null) {
            return;
        }
        if (!isSeedanceReferenceToVideoModel(modelId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "resolution은 Seedance reference-to-video 모델에서만 지원합니다.");
        }
        validateEnumValue("resolution", value, SEEDANCE_RESOLUTION_VALUES);
    }

    private void validateBitrateMode(Object value) {
        if (value == null) {
            return;
        }
        validateEnumValue("bitrate_mode", value, SEEDANCE_BITRATE_MODE_VALUES);
    }

    private void validateEndUserId(Object value) {
        if (value == null || value instanceof String) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "end_user_id는 문자열이어야 합니다.");
    }

    private void validateEnumValue(String key, Object value, Set<String> allowedValues) {
        String stringValue = Objects.toString(value, null);
        if (!allowedValues.contains(stringValue)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    key + "는 다음 값 중 하나여야 합니다: " + String.join(", ", allowedValues)
            );
        }
    }

    private boolean hasMultiPrompt(Map<String, Object> options) {
        if (options == null || !options.containsKey("multi_prompt")) {
            return false;
        }
        Object value = options.get("multi_prompt");
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return value != null;
    }

    private int parseMultiPromptDuration(Object value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt 각 항목에는 duration이 필요합니다.");
        }
        int duration;
        try {
            duration = Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt duration은 숫자여야 합니다.");
        }
        if (duration < 1 || duration > 15) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "multi_prompt duration은 1초 이상 15초 이하여야 합니다.");
        }
        return duration;
    }
}
