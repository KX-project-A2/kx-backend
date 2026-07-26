package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.GenerateJobReferenceMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GenerateJobReferenceMediaRepository extends JpaRepository<GenerateJobReferenceMedia, Long> {

    List<GenerateJobReferenceMedia> findAllByGenerateJob_IdOrderByReferenceTypeAscReferenceOrderAsc(Long generateJobId);

    void deleteByMediaFileIdIn(Collection<Long> mediaFileIds);
}
