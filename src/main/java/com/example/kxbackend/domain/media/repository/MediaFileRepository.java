package com.example.kxbackend.domain.media.repository;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByIdAndUserId(Long id, Long userId);
    Optional<MediaFile> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
    Page<MediaFile> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    Page<MediaFile> findAllByUserIdAndTypeAndDeletedFalse(Long userId, MediaType type, Pageable pageable);
    Page<MediaFile> findAllByUserIdAndGenerateJobIsNotNullAndDeletedFalse(Long userId, Pageable pageable);
    Page<MediaFile> findAllByUserIdAndTypeAndGenerateJobIsNotNullAndDeletedFalse(
            Long userId,
            MediaType type,
            Pageable pageable
    );
    List<MediaFile> findAllByGenerateJob_IdAndDeletedFalseOrderByIdAsc(Long generateJobId);
    List<MediaFile> findAllByGenerateJob_IdInAndDeletedFalseOrderByCreatedAtDescIdAsc(Collection<Long> generateJobIds);
    long countByUserIdAndDeletedFalse(Long userId);
    long countByUserIdAndTypeAndDeletedFalse(Long userId, MediaType type);
    List<MediaFile> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);

    @Query("""
            select max(mediaFile.createdAt)
            from MediaFile mediaFile
            where mediaFile.user.id = :userId
              and mediaFile.deleted = false
            """)
    Optional<LocalDateTime> findLatestCreatedAtByUserId(@Param("userId") Long userId);
}
