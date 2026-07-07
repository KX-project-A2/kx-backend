package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenerateJobRepository extends JpaRepository<GenerateJob, Long> {

    Optional<GenerateJob> findByFalRequestId(String falRequestId);
}
