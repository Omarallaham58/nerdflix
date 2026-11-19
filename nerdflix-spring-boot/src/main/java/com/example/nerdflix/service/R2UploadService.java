package com.example.nerdflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class R2UploadService {//Checked

    private final S3Client s3;
    @Value("${r2.bucket}") private String bucket;

    @Autowired
    public R2UploadService(S3Client s3) {
        this.s3 = s3;
    }

    public void uploadDirectory(Path dir, String prefix) throws IOException {
        Files.walk(dir)
                .filter(Files::isRegularFile)
                // Skip enc.key and key_info.txt
                .filter(file -> {
                    String name = file.getFileName().toString();
                    return !name.equals("enc.key") && !name.equals("key_info.txt");
                })
                .forEach(file -> {
                    try {
                        String key = prefix + "/" + dir.relativize(file).toString().replace("\\", "/");

                        s3.putObject(
                                PutObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .contentType(detectContentType(file))
                                        .build(),
                                file
                        );

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void deleteDirectory(String prefix) {
        System.out.println("Permanently deleting prefix: " + prefix);
        String keyMarker = null;
        String versionIdMarker = null;

        do {
            // We use listObjectVersions to find every version and hide marker
            ListObjectVersionsResponse response = s3.listObjectVersions(ListObjectVersionsRequest.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .keyMarker(keyMarker)
                    .versionIdMarker(versionIdMarker)
                    .build());

            if (response.versions().isEmpty() && response.deleteMarkers().isEmpty()) {
                System.out.println("No versions found.");
                return;
            }

            List<ObjectIdentifier> toDelete = new ArrayList<>();

            // Add all actual file versions to the delete list
            for (ObjectVersion version : response.versions()) {
                System.out.println("Targeting version: " + version.key() + " (" + version.versionId() + ")");
                toDelete.add(ObjectIdentifier.builder()
                        .key(version.key())
                        .versionId(version.versionId())
                        .build());
            }

            // Add all existing hide markers (delete markers) to the delete list
            for (DeleteMarkerEntry marker : response.deleteMarkers()) {
                System.out.println("Targeting hide marker: " + marker.key() + " (" + marker.versionId() + ")");
                toDelete.add(ObjectIdentifier.builder()
                        .key(marker.key())
                        .versionId(marker.versionId())
                        .build());
            }

            // Execute the batch delete
            if (!toDelete.isEmpty()) {
                DeleteObjectsResponse deleteResponse = s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(toDelete).build())
                        .build());

                if (deleteResponse.hasErrors()) {
                    deleteResponse.errors().forEach(err ->
                            System.err.println("Error deleting " + err.key() + ": " + err.message()));
                } else {
                    System.out.println("Batch successfully deleted " + toDelete.size() + " items.");
                }
            }

            keyMarker = response.nextKeyMarker();
            versionIdMarker = response.nextVersionIdMarker();

        } while (keyMarker != null);
    }


    private String detectContentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (name.endsWith(".ts")) return "video/mp2t";
        return "application/octet-stream";
    }

}
