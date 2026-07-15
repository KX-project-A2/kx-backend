package com.example.kxbackend.domain.media.repository;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByIdAndUserId(Long id, Long userId);
    Page<MediaFile> findAllByUserId(Long userId, Pageable pageable);
    Page<MediaFile> findAllByUserIdAndType(Long userId, MediaType type, Pageable pageable);
    List<MediaFile> findAllByGenerateJob_IdOrderByIdAsc(Long generateJobId);
}
