package com.example.nerdflix.repository;

import com.example.nerdflix.entity.VideoLike;
import com.example.nerdflix.entity.VideoLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLikeId> {

    Optional<VideoLike> findByVideoIdAndUserId(UUID videoId, Integer userId);

    boolean existsByVideoIdAndUserIdAndIsLikedTrue(UUID videoId, Integer userId);

    @Query("SELECT COUNT(vl) FROM VideoLike vl WHERE vl.videoId = :videoId AND vl.isLiked = true")
    long countLikesByVideoId(@Param("videoId") UUID videoId);

}