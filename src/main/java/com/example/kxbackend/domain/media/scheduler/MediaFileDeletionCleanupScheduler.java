package com.example.kxbackend.domain.media.scheduler;

import com.example.kxbackend.domain.generate.repository.GenerateJobRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageReferenceRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFavoriteRepository;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.share.repository.ShareLinkRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.infra.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFileDeletionCleanupScheduler {

    private final MediaFileRepository mediaFileRepository;
    private final MediaFavoriteRepository mediaFavoriteRepository;
    private final GenerateJobRepository generateJobRepository;
    private final OpenAiImageReferenceRepository openAiImageReferenceRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final ObjectStorage objectStorage;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${media-file.deletion.retention-days:30}")
    private long retentionDays;

    @Scheduled(cron = "${media-file.deletion.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void collectExpiredDeletedMediaFiles() {
        LocalDateTime threshold = LocalDateTime.now().minus(retentionDuration());
        List<MediaFile> expiredMediaFiles = mediaFileRepository.findAllByDeletedTrueAndDeletedAtBefore(threshold);
        if (expiredMediaFiles.isEmpty()) {
            return;
        }

        log.info("Expired soft-deleted media files found. count={}, threshold={}", expiredMediaFiles.size(), threshold);
        deleteExpiredMediaFiles(expiredMediaFiles);
    }

    private void deleteExpiredMediaFiles(List<MediaFile> expiredMediaFiles) {
        List<MediaFile> deletableMediaFiles = expiredMediaFiles.stream()
                .filter(this::deleteObject)
                .toList();
        if (deletableMediaFiles.isEmpty()) {
            log.info("No expired media files were physically deleted because object deletion failed.");
            return;
        }

        List<Long> mediaFileIds = deletableMediaFiles.stream()
                .map(MediaFile::getId)
                .toList();

        generateJobRepository.clearInputMediaFileReferences(mediaFileIds);
        clearLegacyResultMediaFileReferences(mediaFileIds);
        openAiImageReferenceRepository.deleteByMediaFileIdIn(mediaFileIds);
        shareLinkRepository.deleteByMediaFileIdIn(mediaFileIds);
        mediaFavoriteRepository.deleteByMediaFileIdIn(mediaFileIds);

        for (MediaFile mediaFile : deletableMediaFiles) {
            mediaFileRepository.delete(mediaFile);
        }

        log.info("Expired media files physically deleted. count={}", deletableMediaFiles.size());
    }

    private void clearLegacyResultMediaFileReferences(List<Long> mediaFileIds) {
        if (!hasLegacyResultMediaFileColumn()) {
            return;
        }

        jdbcTemplate.update(
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

    private boolean deleteObject(MediaFile mediaFile) {
        String filePath = mediaFile.getFilePath();
        if (!StringUtils.hasText(filePath)) {
            return true;
        }
        if (isExternalUrl(filePath)) {
            return true;
        }

        try {
            objectStorage.delete(filePath);
            return true;
        } catch (BusinessException exception) {
            log.warn("Media file object deletion failed. mediaFileId={}, filePath={}",
                    mediaFile.getId(), filePath, exception);
            return false;
        }
    }

    private boolean isExternalUrl(String filePath) {
        return filePath.startsWith("http://") || filePath.startsWith("https://");
    }

    private Duration retentionDuration() {
        return Duration.ofDays(Math.max(retentionDays, 1));
    }
}
