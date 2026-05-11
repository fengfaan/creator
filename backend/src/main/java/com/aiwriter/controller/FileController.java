package com.aiwriter.controller;

import com.aiwriter.model.ApiResponse;
import com.aiwriter.model.FileInfo;
import com.aiwriter.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ApiResponse<List<FileInfo>> list() throws Exception {
        return ApiResponse.ok(fileService.listTree());
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<String>> readContent(@RequestParam String path) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(fileService.readContent(path)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "File not found: " + path));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> save(@RequestBody Map<String, String> body) {
        try {
            String path = body.get("path");
            String content = body.get("content");
            boolean existed = fileService.saveContent(path, content);
            return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED)
                    .body(ApiResponse.ok(path));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> create(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String folder = body.get("folder");
            String path = fileService.createFile(name, folder);
            return ResponseEntity.ok(ApiResponse.ok(path));
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "File already exists"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam String path) {
        try {
            fileService.deleteFile(path);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "File not found: " + path));
        }
    }

    @PostMapping("/rename")
    public ResponseEntity<ApiResponse<String>> rename(@RequestBody Map<String, String> body) {
        try {
            String oldPath = body.get("oldPath");
            String newName = body.get("newName");
            String newPath = fileService.renameFile(oldPath, newName);
            return ResponseEntity.ok(ApiResponse.ok(newPath));
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "A file with that name already exists"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "File not found"));
        }
    }
}
