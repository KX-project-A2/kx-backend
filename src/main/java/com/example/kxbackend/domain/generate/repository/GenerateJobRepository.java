package com.example.kxbackend.domain.generate.repository;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.example.kxbackend.domain.generate.entity.enums.Type;

public interface GenerateJobRepository extends JpaRepository<GenerateJob, Long> {

    Optional<GenerateJob> findByFalRequestId(String falRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from GenerateJob job
            where job.id = :jobId
            """)
    Optional<GenerateJob> findByIdForUpdate(@Param("jobId") Long jobId);

    List<GenerateJob> findAllByStatusInAndType(Collection<Status> statuses, Type type);

    List<GenerateJob> findAllByUser_IdAndTypeAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Type type,
            Collection<Status> statuses
    );

    @Query("""
            select distinct job
            from GenerateJob job
            left join fetch job.prompts
            where job.id in :jobIds
            order by job.createdAt desc
            """)
    List<GenerateJob> findAllWithPromptsByIdInOrderByCreatedAtDesc(@Param("jobIds") Collection<Long> jobIds);

    @Query("""
            select job.id
            from GenerateJob job
            where job.user.id = :userId
              and job.status = :status
            order by job.createdAt desc
            """)
    List<Long> findRecentJobIdsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") Status status,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GenerateJob job
            set job.inputMediaFile = null
            where job.inputMediaFile.id in :mediaFileIds
            """)
    int clearInputMediaFileReferences(@Param("mediaFileIds") Collection<Long> mediaFileIds);
}
