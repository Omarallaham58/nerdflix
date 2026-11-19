package com.example.nerdflix.controller;

import com.example.nerdflix.entity.Video;
import com.example.nerdflix.repository.VideoRepository;
import com.example.nerdflix.service.SubscriptionService;
import com.example.nerdflix.service.WorkerTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LicenseController {//Checked

    @Value("${video.encryption-key}")
    private String keyHex;

    private final WorkerTokenService workerTokenService;
    private final VideoRepository videoRepository;
    private final SubscriptionService subscriptionService;

    public LicenseController(
            WorkerTokenService workerTokenService,
            VideoRepository videoRepository,
            SubscriptionService subscriptionService
    ) {
        this.workerTokenService = workerTokenService;
        this.videoRepository = videoRepository;
        this.subscriptionService = subscriptionService;
    }

    // Helper to decode hex to raw 16-byte key
    private byte[] getRawKey() {
        if (keyHex.length() != 32) {
            throw new IllegalStateException("Encryption key must be 32 hex characters (16 bytes)");
        }
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) {
            int index = i * 2;
            int byteValue = Integer.parseInt(keyHex.substring(index, index + 2), 16);
            key[i] = (byte) byteValue;
        }
        return key;
    }

    @GetMapping(value = "/license", produces = "application/octet-stream")
    public ResponseEntity<byte[]> getKey(
            @RequestParam UUID videoId,
            @RequestParam String token
    ) {
        var verified = workerTokenService.verify(token, videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Integer categoryId = video.getCategory().getId();
        boolean subscribed = subscriptionService.isSubscribed(verified.userId(), categoryId);
        if (!subscribed) {
            return ResponseEntity.status(403).build();
        }

        byte[] rawKey = getRawKey();
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(rawKey);
    }
}