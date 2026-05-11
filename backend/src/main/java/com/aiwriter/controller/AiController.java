package com.aiwriter.controller;

import com.aiwriter.model.AiCheckRequest;
import com.aiwriter.model.AiCheckResponse;
import com.aiwriter.model.AiImageRequest;
import com.aiwriter.model.AiImageResponse;
import com.aiwriter.model.AiGenerateRequest;
import com.aiwriter.model.AiGenerateResponse;
import com.aiwriter.model.ApiResponse;
import com.aiwriter.service.AiContentCheckService;
import com.aiwriter.service.AiImageService;
import com.aiwriter.service.AiWritingException;
import com.aiwriter.service.AiWritingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiWritingService aiWritingService;
    private final AiImageService aiImageService;
    private final AiContentCheckService aiContentCheckService;

    public AiController(
            AiWritingService aiWritingService,
            AiImageService aiImageService,
            AiContentCheckService aiContentCheckService
    ) {
        this.aiWritingService = aiWritingService;
        this.aiImageService = aiImageService;
        this.aiContentCheckService = aiContentCheckService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generate(@RequestBody AiGenerateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(aiWritingService.generate(request)));
        } catch (AiWritingException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<AiImageResponse>> generateImage(@RequestBody AiImageRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(aiImageService.generate(request)));
        } catch (AiWritingException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<AiCheckResponse>> check(@RequestBody AiCheckRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(aiContentCheckService.check(request)));
        } catch (AiWritingException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }
}
