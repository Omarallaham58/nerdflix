package com.example.nerdflix.service;

import com.example.nerdflix.dto.response.WorkerToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkerTokenService {

    private final String secret;
    private final long ttlSeconds;

    public WorkerTokenService(
            @Value("${worker.token-secret}") String secret,
            @Value("${worker.token-ttl-seconds:1800}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Issue token WITHOUT IP/User-Agent binding (most reliable).
     *
     * Token format: payloadB64.sigB64
     * Payload: uid=<id>&vid=<uuid>&exp=<epoch>
     *
     * IMPORTANT: We sign payloadB64 (not raw payload),
     * so Worker and backend verify logic stays consistent.
     */
    public WorkerToken issue(Integer userId, UUID videoId, HttpServletRequest request) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;

        String payload = String.format("uid=%d&vid=%s&exp=%d",
                userId, videoId.toString(), exp);

        String payloadB64 = b64Url(payload.getBytes(StandardCharsets.UTF_8));

        // SIGN payloadB64 (not raw payload string)
        String sigB64 = b64Url(hmacSha256(payloadB64));

        String token = payloadB64 + "." + sigB64;

        return new WorkerToken(token, exp);
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign worker token", e);
        }
    }

    private static String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public VerifiedWorkerToken verify(String token, UUID expectedVideoId) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }

        token = token.trim().replaceAll("^\"|\"$", "");

        int dot = token.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("Bad token format");

        String payloadB64 = token.substring(0, dot);
        String sigB64 = token.substring(dot + 1);

        // Verify signature: HMAC(payloadB64)
        String expectedSig = b64Url(hmacSha256(payloadB64));
        if (!expectedSig.equals(sigB64)) {
            throw new IllegalArgumentException("Bad signature");
        }

        String payload = new String(
                Base64.getUrlDecoder().decode(payloadB64),
                StandardCharsets.UTF_8
        );

        Map<String, String> map = new HashMap<>();
        for (String part : payload.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }

        String uidStr = map.get("uid");
        String vidStr = map.get("vid");
        String expStr = map.get("exp");

        if (uidStr == null || vidStr == null || expStr == null) {
            throw new IllegalArgumentException("Missing claims");
        }

        UUID vid = UUID.fromString(vidStr);
        if (!vid.equals(expectedVideoId)) {
            throw new IllegalArgumentException("Video mismatch");
        }

        long exp = Long.parseLong(expStr);
        long now = Instant.now().getEpochSecond();
        if (exp <= now) {
            throw new IllegalArgumentException("Expired");
        }

        int uid = Integer.parseInt(uidStr);
        return new VerifiedWorkerToken(uid, vid, exp);
    }

    public record VerifiedWorkerToken(int userId, UUID videoId, long expEpochSeconds) {}
}