package com.aiwriter.controller;

import com.aiwriter.service.ImageAssetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {
    private final ImageAssetService imageAssetService;

    public AssetController(ImageAssetService imageAssetService) {
        this.imageAssetService = imageAssetService;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> read(HttpServletRequest request) {
        try {
            String prefix = request.getContextPath() + "/api/v1/assets/";
            String uri = request.getRequestURI();
            String assetPath = uri.length() <= prefix.length() ? "" : uri.substring(prefix.length());
            assetPath = URLDecoder.decode(assetPath, StandardCharsets.UTF_8);
            Path file = imageAssetService.resolvePublicAsset(assetPath);
            if (!Files.exists(file) || Files.isDirectory(file)) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(file);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
