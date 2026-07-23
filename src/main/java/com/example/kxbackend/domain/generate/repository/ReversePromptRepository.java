package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.ReversePrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReversePromptRepository extends JpaRepository<ReversePrompt, Long> {

    Optional<ReversePrompt> findByIdAndUser_Id(Long id, Long userId);
}
