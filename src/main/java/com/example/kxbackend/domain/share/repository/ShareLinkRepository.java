package com.example.kxbackend.domain.share.repository;

import com.example.kxbackend.domain.share.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    boolean existsByToken(String token);

    Optional<ShareLink> findByToken(String token);

    Optional<ShareLink> findByIdAndMediaFileIdAndMediaFileUserId(Long id, Long mediaFileId, Long userId);

    void deleteByMediaFileIdIn(Collection<Long> mediaFileIds);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
