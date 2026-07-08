package com.example.kxbackend.domain.media.repository;

import com.example.kxbackend.domain.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByIdAndUserId(Long id, Long userId);
}
