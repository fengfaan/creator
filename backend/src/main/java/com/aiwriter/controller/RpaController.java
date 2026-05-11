package com.aiwriter.controller;

import com.aiwriter.model.ApiResponse;
import com.aiwriter.model.RpaJobResponse;
import com.aiwriter.model.RpaLogEntry;
import com.aiwriter.model.RpaPublishRequest;
import com.aiwriter.rpa.RpaException;
import com.aiwriter.rpa.RpaJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rpa")
public class RpaController {
    private final RpaJobService rpaJobService;

    public RpaController(RpaJobService rpaJobService) {
        this.rpaJobService = rpaJobService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<RpaJobResponse>> start(@RequestBody RpaPublishRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(rpaJobService.start(request)));
        } catch (RpaException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<RpaJobResponse>> get(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(rpaJobService.get(jobId)));
        } catch (RpaException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}/logs")
    public ResponseEntity<ApiResponse<List<RpaLogEntry>>> logs(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") long after
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(rpaJobService.logs(jobId, after)));
        } catch (RpaException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobId}/confirm")
    public ResponseEntity<ApiResponse<RpaJobResponse>> confirm(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(rpaJobService.confirm(jobId)));
        } catch (RpaException e) {
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatus()))
                    .body(ApiResponse.error(e.getStatus(), e.getMessage()));
        }
    }
}
