package com.example.nerdflix.repository;

import com.example.nerdflix.entity.VideoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoStatsRepository extends JpaRepository<VideoStats, UUID> {
    VideoStats findByVideoId(UUID videoId);
}
