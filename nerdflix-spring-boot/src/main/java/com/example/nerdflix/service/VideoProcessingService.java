package com.example.nerdflix.service;

import com.example.nerdflix.dto.request.VideoUpdateForm;
import com.example.nerdflix.entity.Category;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.exception.VideoNotFoundException;
import com.example.nerdflix.repository.VideoRepository;
import com.example.nerdflix.utils.VideoProcessingUtils;
//import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoProcessingService {//Checked

    private final R2UploadService r2Upload;
    private final VideoRepository videoRepo;
    private final VideoStatsService videoStatsService;
    private final VideoProcessingUtils utils;

    @Autowired
    public VideoProcessingService(
            R2UploadService r2Upload,
            VideoRepository videoRepo,
            VideoProcessingUtils utils,
            VideoStatsService videoStatsService )
    {
        this.r2Upload = r2Upload;
        this.videoRepo = videoRepo;
        this.utils = utils;
        this.videoStatsService = videoStatsService;
    }

    @Async
    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<UUID> processAndSave(
            MultipartFile file,
            Video data
    ) throws Exception {
        try {
            UUID videoId = UUID.randomUUID();
            data.setId(videoId);

            // Base directory
            // Path baseDir = Paths.get("/tmp/videos", videoId.toString());
            Path videoRoot = Paths.get(System.getProperty("user.dir"), "videos");
            Path baseDir = videoRoot.resolve(videoId.toString());
            Path workDir = baseDir.resolve("work");
            Path hlsDir = baseDir.resolve("hls");

            Files.createDirectories(workDir);
            Files.createDirectories(hlsDir);

            // Save original file in work dir
            Path input = workDir.resolve("input.mp4");
            file.transferTo(input);

            // Get video Duration
            int duration = utils.getVideoDurationSeconds(input);
            data.setDuration(duration);

            // write encryption key to the file
            byte[] key = utils.getKeyBytes();
            Path keyFile = utils.writeKeyFile(hlsDir);

            // HLS outputs only go into hls/
            utils.generateHls(
                    input,
                    hlsDir.resolve("360p"),
                    keyFile,
                    "http://localhost:8080/api/license?videoId=" + videoId,
                    //"https://<your-worker>.workers.dev/license?videoId=" + videoId,
                    "scale=640:360"
            );

            utils.generateHls(
                    input,
                    hlsDir.resolve("720p"),
                    keyFile,
                    "http://localhost:8080/api/license?videoId=" + videoId,
                    //"https://<your-worker>.workers.dev/license?videoId=" + videoId,
                    "scale=1280:720"
            );

            // Master playlist
            utils.createMasterPlaylist(hlsDir);

            // UPLOAD ONLY HLS
            r2Upload.uploadDirectory(
                    hlsDir,
                    "videos/" + videoId
            );

//            videoRepo.save(data);
            videoRepo.saveAndFlush(data);
            videoStatsService.createStats(data);

            Path p = utils.generateThumbnail(input, data.getId());
            System.out.println("Thumbnail saved to: "+ p);

            return CompletableFuture.completedFuture(videoId);

    

        } catch (Exception e) {
//            throw new RuntimeException(e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional(readOnly=true)
//    @PreAuthorize("isAuthenticated()")
    public Video getVideoById(UUID videoId) {
        return videoRepo.findById(videoId)
                .orElseThrow(() ->
                        new VideoNotFoundException("Video not found: " + videoId)
                );
    }



    public List<Video> findAll() {
        return videoRepo.findAll();
    }

    @Transactional
    public void updateVideo(VideoUpdateForm form, Category category) {

        Video video = videoRepo.findById(form.getId())
                .orElseThrow(() ->
                        new VideoNotFoundException("Video not found")
                );

        video.setTitle(form.getTitle());
        video.setDescription(form.getDescription());
        video.setCategory(category);
    }

    @Transactional
    public void deleteVideo(UUID videoId) {

        Video video = videoRepo.findById(videoId)
                .orElseThrow(() ->
                        new VideoNotFoundException("Video not found: " + videoId)
                );

        // delete from cloud first
        r2Upload.deleteDirectory("videos/" + videoId);

        // delete stats first (FK dependency)
        videoStatsService.deleteByVideoId(videoId);

        // delete video
        videoRepo.delete(video);
    }

}
