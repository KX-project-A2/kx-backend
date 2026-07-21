package com.example.kxbackend.domain.media.scheduler;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFileDeletionCleanupScheduler {

    private final MediaFileRepository mediaFileRepository;

    @Value("${media-file.deletion.retention-days:30}")
    private long retentionDays;

    @Scheduled(cron = "${media-file.deletion.cleanup-cron:0 0 3 * * *}")
    @Transactional(readOnly = true)
    public void collectExpiredDeletedMediaFiles() {
        LocalDateTime threshold = LocalDateTime.now().minus(retentionDuration());
        List<MediaFile> expiredMediaFiles = mediaFileRepository.findAllByDeletedTrueAndDeletedAtBefore(threshold);
        if (expiredMediaFiles.isEmpty()) {
            return;
        }

        log.info("Expired soft-deleted media files found. count={}, threshold={}", expiredMediaFiles.size(), threshold);
    }

    private Duration retentionDuration() {
        return Duration.ofDays(Math.max(retentionDays, 1));
    }
}
