package com.example.nerdflix.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class VideoLikeId implements Serializable {
    private UUID videoId;
    private Integer userId;

    public VideoLikeId() {}

    public VideoLikeId(UUID videoId, Integer userId) {
        this.videoId = videoId;
        this.userId = userId;
    }

    // Getters and setters
    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoLikeId that = (VideoLikeId) o;
        return Objects.equals(videoId, that.videoId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoId, userId);
    }
}