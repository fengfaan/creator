package com.aiwriter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class ImageAssetService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${app.data-dir}")
    private String dataDir;

    @Value("${app.articles-dir:${app.data-dir}/articles}")
    private String articlesDir;

    Path articlesDir() {
        String configured = articlesDir == null || articlesDir.isBlank()
                ? Path.of(dataDir, "articles").toString()
                : articlesDir;
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public SavedImage savePng(byte[] bytes, String purpose) throws IOException {
        return saveImage(bytes, purpose, "png");
    }

    public SavedImage saveJpeg(byte[] bytes, String purpose) throws IOException {
        return saveImage(bytes, purpose, "jpg");
    }

    private SavedImage saveImage(byte[] bytes, String purpose, String extension) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes are empty");
        }
        String safeExtension = safeExtension(extension);
        String safePurpose = safeSegment(purpose);
        String fileName = "%s-%s-%s.%s".formatted(
                safePurpose,
                LocalDateTime.now().format(FILE_TIME),
                UUID.randomUUID().toString().substring(0, 8),
                safeExtension
        );
        Path dir = articlesDir().resolve("assets/images").normalize();
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir)) {
            throw new SecurityException("Path traversal detected");
        }
        Files.createDirectories(dir);
        Files.write(file, bytes);
        String assetPath = "assets/images/" + fileName;
        String publicUrl = "/api/v1/assets/images/" + fileName;
        return new SavedImage(assetPath, publicUrl, file.toString());
    }

    private String safeExtension(String value) {
        String normalized = value == null ? "png" : value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jpg", "jpeg" -> "jpg";
            case "png", "webp" -> normalized;
            default -> "png";
        };
    }

    public Path resolvePublicAsset(String publicAssetPath) {
        if (publicAssetPath == null || publicAssetPath.isBlank()) {
            throw new IllegalArgumentException("Asset path is required");
        }
        Path root = articlesDir().resolve("assets").normalize();
        Path file = root.resolve(publicAssetPath).normalize();
        if (!file.startsWith(root)) {
            throw new SecurityException("Path traversal detected");
        }
        return file;
    }

    private String safeSegment(String value) {
        String normalized = value == null ? "image" : value.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "image" : normalized;
    }

    public record SavedImage(String assetPath, String publicUrl, String absolutePath) {
    }
}
