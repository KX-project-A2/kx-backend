package com.example.kxbackend.domain.share.scheduler;

import com.example.kxbackend.domain.share.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShareLinkCleanupScheduler {

    private final ShareLinkRepository shareLinkRepository;

    @Scheduled(cron = "${share.link.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpiredShareLinks() {
        long deletedCount = shareLinkRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Expired share links deleted. count={}", deletedCount);
        }
    }
}
