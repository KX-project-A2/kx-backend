package com.example.kxbackend.domain.media.repository;

import com.example.kxbackend.domain.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findAllByGenerateJob_IdOrderByIdAsc(Long generateJobId);
}
