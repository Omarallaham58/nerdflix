package com.example.nerdflix.controller;

import com.example.nerdflix.entity.Category;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.exception.CategoryNotFoundException;
import com.example.nerdflix.repository.CategoryRepository;
import com.example.nerdflix.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/test/videos")
public class VideoUploadTestController {//Test only

    private final VideoProcessingService videoService;
    private final CategoryRepository categoryRepository;

    @Autowired
    public VideoUploadTestController(VideoProcessingService service, CategoryRepository categoryRepo){
        videoService = service;
        this.categoryRepository = categoryRepo;
    }

    @PostMapping("/upload")
//    public CompletableFuture<ResponseEntity<String>> upload(
    public ResponseEntity<String> upload(

            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int categoryId
    ) throws Exception {

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + categoryId));;

        video.setCategory(cat);
        videoService.processAndSave(file, video);


        return ResponseEntity.ok(
                "Upload started (processing async)"
        );
    }
}

