package com.example.nerdflix.service;

import com.example.nerdflix.entity.Video;
import com.example.nerdflix.entity.VideoLike;
import com.example.nerdflix.entity.VideoStats;
import com.example.nerdflix.exception.VideoNotFoundException;
import com.example.nerdflix.repository.VideoLikeRepository;
import com.example.nerdflix.repository.VideoRepository;
import com.example.nerdflix.repository.VideoStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class VideoLikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoRepository videoRepository;
    private final VideoStatsRepository videoStatsRepository;

    public VideoLikeService(VideoLikeRepository videoLikeRepository,
                            VideoRepository videoRepository,
                            VideoStatsRepository videoStatsRepository) {
        this.videoLikeRepository = videoLikeRepository;
        this.videoRepository = videoRepository;
        this.videoStatsRepository = videoStatsRepository;
    }




    @Transactional
    public void likeVideo(UUID videoId, Integer userId) {
        // Verify video exists
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found: " + videoId));

        // Check if there's an existing record
        Optional<VideoLike> existingLike = videoLikeRepository.findByVideoIdAndUserId(videoId, userId);

        if (existingLike.isPresent()) {
            VideoLike videoLike = existingLike.get();
            if (videoLike.getIsLiked()) {
                return; // Already liked
            }
            videoLike.setIsLiked(true);
            // No need to save - it's managed
        } else {
            // New like - must save
            VideoLike videoLike = new VideoLike(videoId, userId);
            videoLike.setIsLiked(true);
            videoLikeRepository.saveAndFlush(videoLike);
        }

        // Update stats - stats is managed, so no save needed
        VideoStats stats = videoStatsRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Stats not found for video: " + videoId));
        stats.setLikes(stats.getLikes() + 1);
    }


    @Transactional
    public void unlikeVideo(UUID videoId, Integer userId) {
        // Verify video exists
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found: " + videoId));

        // Check if there's an existing record
        Optional<VideoLike> existingLike = videoLikeRepository.findByVideoIdAndUserId(videoId, userId);
        boolean isPresent= false;
        if (existingLike.isPresent()) {
            isPresent = true;
            VideoLike videoLike = existingLike.get();
            if (videoLike.getIsLiked() == false) {
                return;
            }
            videoLike.setIsLiked(false);
            // No need to save - it's managed
        }

        // Update stats - stats is managed, so no save needed
        VideoStats stats = videoStatsRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Stats not found for video: " + videoId));

        if (isPresent) stats.setLikes(Math.max(0,stats.getLikes() - 1));
    }

//    @Transactional
//    public void likeVideo(UUID videoId, Integer userId) {
//
//        videoLikeRepository.updateLikeStatus(videoId,userId,true);
//    }


//    @Transactional
//    public void likeVideo(UUID videoId, Integer userId) {
//        // First, try to update existing row
//        int updatedRows = videoLikeRepository.updateLikeStatus(videoId, userId, true);
//
//        if (updatedRows == 0) {
//            // No existing row, so insert a new one
//            VideoLike newLike = new VideoLike(videoId, userId);
//            videoLikeRepository.save(newLike);
//        }
//
//        // Always increment the stats
//        VideoStats stats = videoStatsRepository.findById(videoId)
//                .orElseThrow(() -> new RuntimeException("Stats not found for video: " + videoId));
//        stats.setLikes(stats.getLikes() + 1);
//    }


//    @Transactional
//    public void unlikeVideo(UUID videoId, Integer userId) {
//        // Try to update existing row to false
//        int updatedRows = videoLikeRepository.updateLikeStatus(videoId, userId, false);
//
//        if (updatedRows > 0) {
//            // Only decrement if there was actually a like to remove
//            VideoStats stats = videoStatsRepository.findById(videoId)
//                    .orElseThrow(() -> new RuntimeException("Stats not found for video: " + videoId));
//            stats.setLikes(Math.max(0, stats.getLikes() - 1));
//        }
//        // If updatedRows == 0, user hadn't liked it anyway, so do nothing
//    }


//    @Transactional
//    public void unlikeVideo(UUID videoId, Integer userId) {
//        VideoLike videoLike = videoLikeRepository.findByVideoIdAndUserId(videoId, userId)
//                .orElse(null);
//
//        if (videoLike == null || !videoLike.getIsLiked()) {
//            return; // Not liked
//        }
//
//
//        videoLike.setIsLiked(false);
//        videoLikeRepository.saveAndFlush(videoLike);
//
//        // Decrement the like count
//        VideoStats stats = videoStatsRepository.findById(videoId)
//                .orElseThrow(() -> new RuntimeException("Stats not found for video: " + videoId));
//        stats.setLikes(Math.max(0, stats.getLikes() - 1));
//        videoStatsRepository.saveAndFlush(stats);
//    }

    @Transactional(readOnly = true)
    public boolean hasUserLikedVideo(UUID videoId, Integer userId) {
       // return videoLikeRepository.existsByVideoIdAndUserIdAndIsLikedTrue(videoId, userId);
        Optional<VideoLike> videoLike = videoLikeRepository.findByVideoIdAndUserId(videoId,userId);
      return (videoLike.isPresent())?videoLike.get().getIsLiked():false;
    }

    @Transactional(readOnly = true)
    public long getLikeCount(UUID videoId) {
        return videoLikeRepository.countLikesByVideoId(videoId);
    }
}