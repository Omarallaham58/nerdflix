package com.example.nerdflix.controller;

import com.example.nerdflix.dto.response.StreamInfo;
import com.example.nerdflix.dto.response.VideoDTO;
import com.example.nerdflix.dto.response.WorkerToken;
import com.example.nerdflix.service.JwtTokenService;
import com.example.nerdflix.service.VideoLikeService;
import com.example.nerdflix.service.VideoService;
import com.example.nerdflix.service.WorkerTokenService;
import com.example.nerdflix.utils.VideoProcessingUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class VideoController {//Checked

    private final VideoProcessingUtils utils;
    private final VideoService videoService;
    private final JwtTokenService jwtTokenService;
    private final WorkerTokenService workerTokenService;
    private final VideoLikeService videoLikeService;

    @Value("${worker.base-url}")
    private String workerBaseUrl;

    public VideoController(VideoProcessingUtils utils,
                           VideoService videoService,
                           JwtTokenService jwtTokenService,
                           WorkerTokenService workerTokenService,
                           VideoLikeService videoLikeService) {
        this.utils = utils;
        this.videoService = videoService;
        this.jwtTokenService = jwtTokenService;
        this.workerTokenService = workerTokenService;
        this.videoLikeService=videoLikeService;
    }

    @GetMapping("/videos")
    public List<VideoDTO> getAllVideos() {
        return videoService.getAll();
    }

    @GetMapping("/videos/user")
    public List<VideoDTO> getVideosByUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);
        return videoService.getVideosByUserId(userId);
    }

    @GetMapping("/videos/{videoId}")
    public VideoDTO getVideoById(@PathVariable UUID videoId,
                                 @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);
        return videoService.getVideoById(videoId, userId);
    }

    /**
     * Frontend asks backend for a worker URL (manifest URL) + expiry.
     * Backend validates subscription BEFORE issuing the worker token.
     * includes IP and User-Agent binding.
     */
    @GetMapping("/videos/{videoId}/stream-info")
    public StreamInfo getStreamInfo(@PathVariable UUID videoId,
                                    @RequestHeader("Authorization") String authHeader,
                                    HttpServletRequest request) {

        String jwt = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(jwt);

        // Authorization: must be subscribed
        videoService.getVideoById(videoId, userId);

        // Pass the request to get IP and User-Agent for binding
        WorkerToken wt = workerTokenService.issue(userId, videoId, request);

        String manifestUrl = workerBaseUrl + "/stream/" + videoId + "/master.m3u8?token=" + wt.token();

        return new StreamInfo(manifestUrl, wt.expEpochSeconds());
    }

    /**
     * Validate that the worker token matches the current request context
     */
    private void validateTokenBinding(String workerToken, HttpServletRequest request) throws AccessDeniedException {
        try {
            // Decode token payload
            String payloadB64 = workerToken.split("\\.")[0];
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64));

            Map<String, String> claims = Arrays.stream(payload.split("&"))
                    .map(p -> p.split("="))
                    .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));

            String tokenIp = claims.get("ip");
            String tokenUa = claims.get("ua");

            // Get current request info
            String currentIp = getClientIp(request);
            String currentUa = hashUserAgent(request.getHeader("User-Agent"));

            // Validate IP (allow /24 subnet for dynamic IPs)
            if (!ipsMatch(tokenIp, currentIp)) {
                // Log but don't necessarily block - could be NAT
                System.out.println("IP mismatch - Token: " + tokenIp + ", Current: " + currentIp);
                // For strict security, uncomment:
                 throw new AccessDeniedException("IP address changed");
            }

            // Validate User-Agent (should match exactly)
            if (!tokenUa.equals(currentUa)) {
                System.out.println("User-Agent mismatch - Token: " + tokenUa + ", Current: " + currentUa);
                throw new AccessDeniedException("User-Agent changed - possible token theft");
            }

        } catch (Exception e) {
            throw new AccessDeniedException("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Get real client IP (same as in WorkerTokenService)
     */
    private String getClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            return cfConnectingIp;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Hash User-Agent (same as in WorkerTokenService)
     */
    private String hashUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);
        } catch (Exception e) {
            return userAgent.contains("Chrome") ? "chrome" : "other";
        }
    }

    /**
     * Check if two IPs are in the same /24 subnet
     */
    private boolean ipsMatch(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) return false;
        if (ip1.equals(ip2)) return true;

        // Check if they're in same /24 subnet
        String subnet1 = ip1.substring(0, ip1.lastIndexOf('.'));
        String subnet2 = ip2.substring(0, ip2.lastIndexOf('.'));
        return subnet1.equals(subnet2);
    }

    @PostMapping("/videos/{videoId}/like")
    public ResponseEntity<Void> likeVideo(@PathVariable UUID videoId,
                                          @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);

        // Verify subscription first
        videoService.getVideoById(videoId, userId);

        // Perform like operation
        videoLikeService.likeVideo(videoId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/videos/{videoId}/unlike")
    public ResponseEntity<Void> unlikeVideo(@PathVariable UUID videoId,
                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);

        // Verify subscription first
        videoService.getVideoById(videoId, userId);

        // Perform unlike operation
        videoLikeService.unlikeVideo(videoId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/videos/{videoId}/view")
    public ResponseEntity<Void> trackView(@PathVariable UUID videoId,
                                          @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);
        System.out.println("Incrementing view count for video " + videoId + " by user "+userId);
        videoService.incrementView(videoId, userId);
        return ResponseEntity.ok().build();
    }

}