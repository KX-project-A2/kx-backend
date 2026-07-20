package com.example.kxbackend.domain.media.repository;

import com.example.kxbackend.domain.media.entity.MediaFavorite;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaFavoriteRepository extends JpaRepository<MediaFavorite, Long> {

    boolean existsByUserIdAndMediaFileId(Long userId, Long mediaFileId);

    Optional<MediaFavorite> findByUserIdAndMediaFileId(Long userId, Long mediaFileId);

    void deleteByUserIdAndMediaFileId(Long userId, Long mediaFileId);

    @Query("""
            select favorite.mediaFile.id
            from MediaFavorite favorite
            where favorite.user.id = :userId
              and favorite.mediaFile.id in :mediaFileIds
            """)
    List<Long> findFavoriteMediaFileIds(
            @Param("userId") Long userId,
            @Param("mediaFileIds") Collection<Long> mediaFileIds
    );

    @Query("""
            select favorite.mediaFile
            from MediaFavorite favorite
            where favorite.user.id = :userId
            order by favorite.mediaFile.createdAt desc
            """)
    Page<MediaFile> findFavoriteMediaFilesByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            select favorite.mediaFile
            from MediaFavorite favorite
            where favorite.user.id = :userId
              and favorite.mediaFile.type = :type
            order by favorite.mediaFile.createdAt desc
            """)
    Page<MediaFile> findFavoriteMediaFilesByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") MediaType type,
            Pageable pageable
    );
}
