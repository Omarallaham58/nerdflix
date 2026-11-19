package com.example.nerdflix.service;

import com.example.nerdflix.dto.request.UpdateVideoStatRequest;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.entity.VideoStats;
import com.example.nerdflix.repository.VideoStatsRepository;
//import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VideoStatsService {//Checked

    private final VideoStatsRepository statsRepo;

    @Autowired
    public VideoStatsService(VideoStatsRepository statsRepo) {
        this.statsRepo = statsRepo;
    }

    @Transactional
    public void createStats(Video video) {

        VideoStats stats = new VideoStats();
        stats.setVideo(video);
        stats.setVideoId(video.getId());
        stats.setViews(0);
        stats.setLikes(0);

        statsRepo.save(stats);
    }

    @Transactional
    public void deleteByVideoId(UUID videoId) {
        statsRepo.deleteById(videoId);
    }

    @Transactional(readOnly = true)
    public VideoStats getStatsByVideoId(UUID id) {
        return statsRepo.findByVideoId(id);
    }


    //update Stat
    @Transactional
    public VideoStats updateStat(UUID videoId, UpdateVideoStatRequest  request) {

        VideoStats stats = statsRepo.findByVideoId(videoId);
        stats.setLikes(request.likes());
        stats.setViews(request.views());
        return statsRepo.saveAndFlush(stats);


    }

}
