package com.example.nerdflix.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class VideoProcessingUtils {

    @Value("${video.encryption-key}")
    private String keyHex;  // 32 hex characters (16-byte AES-128 key), from VIDEO_ENCRYPTION_KEY

    // Convert hex string to raw 16-byte key
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

    public byte[] getKeyBytes() {
        return getRawKey();
    }

    public Path writeKeyFile(Path videoTempDir) throws IOException {
        Path keyFile = videoTempDir.resolve("enc.key");
        Files.write(keyFile, getRawKey()); // write raw 16 bytes
        return keyFile;
    }

    // ---------------------------------FFMPEG SERVICES--------------------------------------------

    public void generateHls(
            Path inputVideo,
            Path outputDir,
            Path keyFile,
            String keyUri,
            String resolution
    ) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        Path keyInfo = outputDir.resolve("key_info.txt");
        Files.writeString(keyInfo,
                keyUri + "\n" +
                        keyFile.toAbsolutePath() + "\n"
        );
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputVideo.toString(),
                "-vf", resolution,
                "-hls_time", "4",
                "-hls_key_info_file", keyInfo.toString(),   // 👈 Encryption re‑enabled
                "-hls_playlist_type", "vod",
                "-hls_segment_filename",
                outputDir + "/segment_%03d.ts",
                outputDir + "/index.m3u8"
        );
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }

    public void createMasterPlaylist(Path videoDir) throws IOException {
        String content = """
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
        360p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720
        720p/index.m3u8
        """;
        Files.writeString(videoDir.resolve("master.m3u8"), content);
    }

    public int getVideoDurationSeconds(Path videoPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath.toAbsolutePath().toString()
        );
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            if (line == null) {
                throw new RuntimeException("Could not read video duration");
            }
            double seconds = Double.parseDouble(line);
            return (int) Math.round(seconds);
        }
    }

    public Path generateThumbnail(Path videoPath, UUID videoId) throws Exception {
        Path thumbnailsDir = Paths.get(System.getProperty("user.dir"), "thumbnails");
        Files.createDirectories(thumbnailsDir);
        Path thumbnailPath = thumbnailsDir.resolve(videoId + ".jpg");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", videoPath.toAbsolutePath().toString(),
                "-ss", "00:00:01",
                "-frames:v", "1",
                "-update", "1",
                thumbnailPath.toAbsolutePath().toString()
        );
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();
        return thumbnailPath;
    }
}