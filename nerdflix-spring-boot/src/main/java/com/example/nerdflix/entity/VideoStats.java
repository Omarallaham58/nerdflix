package com.example.nerdflix.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "video_stats", schema="nerdflix")
public class VideoStats {

    @Id
    @Column(name = "video_id")
    private UUID videoId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Column(nullable = false)
    private Integer views = 0;

    @Column(nullable = false)
    private Integer likes = 0;  // This will be a denormalized count

    @Version
    private Long version;

    @Transient  // Not stored in DB, computed on the fly
    private Boolean likedByCurrentUser;

    // Constructors, getters, setters...

    public VideoStats(){}

    public VideoStats(UUID videoId, Video video, Integer views, Integer likes, Long version, Boolean likedByCurrentUser) {
        this.videoId = videoId;
        this.video = video;
        this.views = views;
        this.likes = likes;
        this.version = version;
        this.likedByCurrentUser = likedByCurrentUser;
    }


    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "VideoStats{" +
                "videoId=" + videoId +
                ", video=" + video +
                ", views=" + views +
                ", likes=" + likes +
                ", version=" + version +
                ", likedByCurrentUser=" + likedByCurrentUser +
                '}';
    }

    public Boolean getLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(Boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }
}