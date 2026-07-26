package com.example.kxbackend.domain.user.service;

import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.dto.request.ProfileUpdateRequestDto;
import com.example.kxbackend.domain.user.dto.response.GenerationSummaryResponseDto;
import com.example.kxbackend.domain.user.dto.response.ProfileResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.openai.OpenAiReferenceImageClient;
import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService;
import com.example.kxbackend.infra.storage.service.ImageUploadStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ImageUploadStorageService imageUploadStorageService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectStorage objectStorage;
    private final OpenAiReferenceImageClient openAiReferenceImageClient;

    public ProfileResponseDto getProfile(Long userId) {
        return toProfileResponse(getUser(userId));
    }

    @Transactional
    public ProfileResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = getUser(userId);
        user.updateNickname(request.nickname());
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponseDto uploadProfileImage(Long userId, MultipartFile file) {
        User user = getUser(userId);
        String oldProfileImagePath = user.getProfileImagePath();
        String newProfileImagePath = imageUploadStorageService.uploadProfileImage(userId, file);

        user.updateProfileImagePath(newProfileImagePath);
        deleteOldProfileImageIfExists(oldProfileImagePath);
        return toProfileResponse(user);
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = getUser(userId);
        String profileImagePath = user.getProfileImagePath();
        if (!StringUtils.hasText(profileImagePath)) {
            return;
        }

        imageUploadStorageService.delete(profileImagePath);
        user.deleteProfileImage();
    }

    public GenerationSummaryResponseDto getSummary(Long userId) {
        return new GenerationSummaryResponseDto(
                mediaFileRepository.countByUserIdAndDeletedFalse(userId),
                mediaFileRepository.countByUserIdAndTypeAndDeletedFalse(userId, MediaType.IMAGE),
                mediaFileRepository.countByUserIdAndTypeAndDeletedFalse(userId, MediaType.VIDEO),
                mediaFileRepository.findLatestCreatedAtByUserId(userId).orElse(null)
        );
    }

    public void deleteAccount(Long userId) {
        DeletionTargets targets = collectDeletionTargets(userId);

        transactionTemplate.executeWithoutResult(status -> deleteDatabaseRows(userId, targets));

        deleteObjects(targets.objectKeys());
        deleteOpenAiReferenceFiles(targets.openAiFileIds());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private DeletionTargets collectDeletionTargets(Long userId) {
        User user = getUser(userId);
        List<Long> mediaFileIds = findLongList(
                "select id from media_file where user_id = :userId",
                Map.of("userId", userId)
        );
        List<Long> generateJobIds = findLongList(
                "select id from generate_job where user_id = :userId",
                Map.of("userId", userId)
        );

        List<String> objectKeys = new ArrayList<>();
        if (StringUtils.hasText(user.getProfileImagePath())) {
            objectKeys.add(user.getProfileImagePath());
        }
        objectKeys.addAll(findStringList(
                "select file_path from media_file where user_id = :userId and file_path is not null",
                Map.of("userId", userId)
        ));

        List<String> openAiFileIds = findOpenAiFileIds(generateJobIds, mediaFileIds);

        return new DeletionTargets(mediaFileIds, generateJobIds, objectKeys, openAiFileIds);
    }

    private List<String> findOpenAiFileIds(List<Long> generateJobIds, List<Long> mediaFileIds) {
        if (generateJobIds.isEmpty() && mediaFileIds.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("select distinct openai_file_id from openai_image_reference where ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (!generateJobIds.isEmpty()) {
            conditions.add("generate_job_id in (:generateJobIds)");
            params.addValue("generateJobIds", generateJobIds);
        }
        if (!mediaFileIds.isEmpty()) {
            conditions.add("media_file_id in (:mediaFileIds)");
            params.addValue("mediaFileIds", mediaFileIds);
        }

        sql.append(String.join(" or ", conditions));
        return jdbcTemplate.queryForList(sql.toString(), params, String.class);
    }

    private void deleteDatabaseRows(Long userId, DeletionTargets targets) {
        List<Long> mediaFileIds = targets.mediaFileIds();
        List<Long> generateJobIds = targets.generateJobIds();

        update("delete from refresh_token where user_id = :userId", Map.of("userId", userId));

        if (!mediaFileIds.isEmpty()) {
            MapSqlParameterSource mediaParams = new MapSqlParameterSource("mediaFileIds", mediaFileIds);
            update("delete from share_link where media_file_id in (:mediaFileIds)", mediaParams);
            update("delete from media_favorite where user_id = :userId or media_file_id in (:mediaFileIds)",
                    mediaParams.addValue("userId", userId));
            update("delete from generate_job_reference_media where media_file_id in (:mediaFileIds)", mediaParams);
            update("delete from openai_image_reference where media_file_id in (:mediaFileIds)", mediaParams);
            update("update generate_job set input_media_file_id = null where input_media_file_id in (:mediaFileIds)",
                    mediaParams);
            clearLegacyResultMediaFileReferences(mediaFileIds);
            update("delete from media_file where id in (:mediaFileIds)", mediaParams);
        } else {
            update("delete from media_favorite where user_id = :userId", Map.of("userId", userId));
        }

        if (!generateJobIds.isEmpty()) {
            MapSqlParameterSource jobParams = new MapSqlParameterSource("generateJobIds", generateJobIds);
            update("delete from generate_job_reference_media where generate_job_id in (:generateJobIds)", jobParams);
            update("delete from openai_image_reference where generate_job_id in (:generateJobIds)", jobParams);
            update("delete from openai_image_generate_job_option where generate_job_id in (:generateJobIds)", jobParams);
            update("delete from generate_prompt where generate_job_id in (:generateJobIds)", jobParams);
            update("delete from generate_job where id in (:generateJobIds)", jobParams);
        }

        update("delete from users where id = :userId", Map.of("userId", userId));
    }

    private void clearLegacyResultMediaFileReferences(List<Long> mediaFileIds) {
        if (!hasLegacyResultMediaFileColumn()) {
            return;
        }

        update(
                """
                        update generate_job
                        set result_media_file_id = null
                        where result_media_file_id in (:mediaFileIds)
                        """,
                new MapSqlParameterSource("mediaFileIds", mediaFileIds)
        );
    }

    private boolean hasLegacyResultMediaFileColumn() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'generate_job'
                          and column_name = 'result_media_file_id'
                        """,
                new MapSqlParameterSource(),
                Integer.class
        );
        return count != null && count > 0;
    }

    private void deleteObjects(List<String> objectKeys) {
        objectKeys.stream()
                .filter(StringUtils::hasText)
                .map(ObjectStorageKeys::normalize)
                .filter(objectKey -> !isExternalUrl(objectKey))
                .distinct()
                .forEach(this::deleteObject);
    }

    private void deleteObject(String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("계정 삭제 후 object 삭제 실패. objectKey={}", objectKey, exception);
        }
    }

    private void deleteOpenAiReferenceFiles(List<String> openAiFileIds) {
        openAiFileIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(this::deleteOpenAiReferenceFile);
    }

    private void deleteOpenAiReferenceFile(String openAiFileId) {
        try {
            openAiReferenceImageClient.deleteReferenceImage(openAiFileId);
        } catch (RuntimeException exception) {
            log.warn("계정 삭제 후 OpenAI 레퍼런스 파일 삭제 실패. fileId={}", openAiFileId, exception);
        }
    }

    private boolean isExternalUrl(String objectKey) {
        return objectKey.startsWith("http://") || objectKey.startsWith("https://");
    }

    private List<Long> findLongList(String sql, Map<String, ?> params) {
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }

    private List<String> findStringList(String sql, Map<String, ?> params) {
        return jdbcTemplate.queryForList(sql, params, String.class);
    }

    private int update(String sql, Map<String, ?> params) {
        return jdbcTemplate.update(sql, params);
    }

    private int update(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.update(sql, params);
    }

    private ProfileResponseDto toProfileResponse(User user) {
        return ProfileResponseDto.from(user, createProfileImageUrl(user.getProfileImagePath()));
    }

    private String createProfileImageUrl(String profileImagePath) {
        if (!StringUtils.hasText(profileImagePath)) {
            return null;
        }

        String objectKey = ObjectStorageKeys.normalize(profileImagePath);
        return s3PresignedUrlService.createReadUrl(objectKey).url();
    }

    private void deleteOldProfileImageIfExists(String oldProfileImagePath) {
        if (!StringUtils.hasText(oldProfileImagePath)) {
            return;
        }

        try {
            imageUploadStorageService.delete(oldProfileImagePath);
        } catch (BusinessException exception) {
            log.warn("이전 프로필 이미지 삭제 실패. objectKey={}", oldProfileImagePath, exception);
        }
    }

    private record DeletionTargets(
            List<Long> mediaFileIds,
            List<Long> generateJobIds,
            List<String> objectKeys,
            List<String> openAiFileIds
    ) {
    }
}
