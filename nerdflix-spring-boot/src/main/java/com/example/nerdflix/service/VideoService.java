package com.example.nerdflix.service;

import com.example.nerdflix.dto.request.UpdateVideoStatRequest;
import com.example.nerdflix.dto.response.CategoryDTO;
import com.example.nerdflix.dto.response.VideoDTO;
import com.example.nerdflix.dto.response.VideoStatsDTO;
import com.example.nerdflix.entity.Subscription;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.entity.VideoStats;
import com.example.nerdflix.exception.VideoNotFoundException;
import com.example.nerdflix.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {
    private final VideoRepository videoRepo;
    private final VideoStatsService statsService;
    private final SubscriptionService subscriptionService;
    private final VideoLikeService videoLikeService;  // Add this

    @Autowired
    public VideoService(VideoRepository videoRepo,
                        VideoStatsService statsService,
                        SubscriptionService subscriptionService,
                        VideoLikeService videoLikeService) {  // Add parameter
        this.videoRepo = videoRepo;
        this.statsService = statsService;
        this.subscriptionService = subscriptionService;
        this.videoLikeService = videoLikeService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public VideoDTO getVideoById(UUID videoId, int userId) {
        Video video = videoRepo.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("No Video!"));

        //testing
        System.out.println("Video :  " +  video.toString());
        validateUserSubscription(userId, video.getCategory().getId());

        VideoStats stats = statsService.getStatsByVideoId(videoId);
        System.out.println("Stats :  " +  stats.toString());
        // Check if current user liked this video
        boolean likedByCurrentUser = videoLikeService.hasUserLikedVideo(videoId, userId);
        System.out.println("likedByCurrent user: " + likedByCurrentUser);

        return new VideoDTO(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getDuration(),
                video.getCreatedAt(),
                new CategoryDTO(
                        video.getCategory().getId(),
                        video.getCategory().getName().toString(),
                        video.getCategory().getDescription()
                ),
                new VideoStatsDTO(stats.getViews(), stats.getLikes(), likedByCurrentUser)  // Add likedByCurrentUser
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public List<VideoDTO> getVideosByUserId(Integer userId) {
        List<Subscription> subscriptions = subscriptionService.getSubscriptionsByUserId(userId);
        List<Integer> categoryIds = subscriptions.stream()
                .map(Subscription::getCategoryId)
                .toList();

        List<Video> videos = videoRepo.findAllByCategoryIds(categoryIds);

        return videos.stream().map(video -> {
            VideoStats stats = statsService.getStatsByVideoId(video.getId());
            boolean likedByCurrentUser = videoLikeService.hasUserLikedVideo(video.getId(), userId);

            return new VideoDTO(
                    video.getId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getDuration(),
                    video.getCreatedAt(),
                    new CategoryDTO(
                            video.getCategory().getId(),
                            video.getCategory().getName().toString(),
                            video.getCategory().getDescription()
                    ),
                    new VideoStatsDTO(stats.getViews(), stats.getLikes(), likedByCurrentUser)
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<VideoDTO> getAll() {
        List<Video> videos = videoRepo.findAll();

        // For public endpoints, no user-specific like status
        return videos.stream()
                .map(video -> {
                    VideoStats stats = statsService.getStatsByVideoId(video.getId());
                    return new VideoDTO(
                            video.getId(),
                            video.getTitle(),
                            video.getDescription(),
                            video.getDuration(),
                            video.getCreatedAt(),
                            new CategoryDTO(
                                    video.getCategory().getId(),
                                    video.getCategory().getName().toString(),
                                    video.getCategory().getDescription()
                            ),
                            new VideoStatsDTO(stats.getViews(), stats.getLikes(), null)  // null for public
                    );
                })
                .toList();
    }



    @Transactional
    //preauthorize for this user having userId
    @PreAuthorize("#userId == authentication.principal.userId")
    public void incrementView(UUID videoId, int userId) {
        System.out.println("Increment view method entered for video id = "
                + videoId.toString() + " and userid = " + userId );
        Video video = videoRepo.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("No Video!"));

        validateUserSubscription(userId, video.getCategory().getId());
        VideoStats stats = statsService.getStatsByVideoId(videoId);
       // stats.setViews(stats.getViews() + 1);
        System.out.println("Stat before update: " +  stats.toString());
        stats = statsService.updateStat(videoId,new UpdateVideoStatRequest(stats.getViews()+1,stats.getLikes()));
        System.out.println("Stats after update: " +  stats.toString());
    }



    public void validateUserSubscription(Integer userId, Integer categoryId) {

        boolean subscribed = subscriptionService
                .isSubscribed(userId, categoryId);
        System.out.println("Subscribed: " + subscribed);
        if (!subscribed) {
            throw new AccessDeniedException("User is not subscribed to this category");
        }
    }

}
