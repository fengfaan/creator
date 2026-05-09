package com.aiwriter.service;

import com.aiwriter.model.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${app.data-dir}")
    private String dataDir;

    private Path articlesDir() {
        return Path.of(dataDir, "articles").toAbsolutePath().normalize();
    }

    private Path resolve(String relativePath) {
        Path resolved = articlesDir().resolve(relativePath).normalize();
        if (!resolved.startsWith(articlesDir())) {
            throw new SecurityException("Path traversal detected");
        }
        return resolved;
    }

    public List<FileInfo> listTree() throws IOException {
        Path root = articlesDir();
        if (!Files.exists(root)) return List.of();
        List<FileInfo> children = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.sorted().forEach(p -> {
                FileInfo node = buildNode(p, "");
                if (node != null) children.add(node);
            });
        }
        return List.of(FileInfo.builder()
                .id("root")
                .name("我的文稿")
                .path("")
                .type("folder")
                .children(children)
                .build());
    }

    private FileInfo buildNode(Path path, String parentRel) {
        String name = path.getFileName().toString();
        String rel = parentRel.isEmpty() ? name : parentRel + "/" + name;
        if (Files.isDirectory(path)) {
            List<FileInfo> children = new ArrayList<>();
            try (var stream = Files.list(path)) {
                stream.sorted().forEach(p -> {
                    FileInfo node = buildNode(p, rel);
                    if (node != null) children.add(node);
                });
            } catch (IOException e) {
                log.error("Failed to list directory: {}", path, e);
            }
            return FileInfo.builder()
                    .id(UUID.nameUUIDFromBytes(rel.getBytes()).toString())
                    .name(name)
                    .path(rel)
                    .type("folder")
                    .children(children)
                    .build();
        }
        if (name.endsWith(".md")) {
            return FileInfo.builder()
                    .id(UUID.nameUUIDFromBytes(rel.getBytes()).toString())
                    .name(name)
                    .path(rel)
                    .type("file")
                    .build();
        }
        return null;
    }

    public String readContent(String relativePath) throws IOException {
        Path file = resolve(relativePath);
        if (!Files.exists(file)) throw new NoSuchFileException(file.toString());
        return Files.readString(file);
    }

    public boolean saveContent(String relativePath, String content) throws IOException {
        Path file = resolve(relativePath);
        boolean existed = Files.exists(file);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return existed;
    }

    public String createFile(String name, String folder) throws IOException {
        String rel = (folder == null || folder.isEmpty()) ? name : folder + "/" + name;
        if (!rel.endsWith(".md")) rel += ".md";
        Path file = resolve(rel);
        if (Files.exists(file)) throw new FileAlreadyExistsException(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "");
        return rel;
    }

    public void deleteFile(String relativePath) throws IOException {
        Path file = resolve(relativePath);
        if (!Files.exists(file)) throw new NoSuchFileException(file.toString());
        Files.delete(file);
    }

    public String renameFile(String oldPath, String newName) throws IOException {
        Path old = resolve(oldPath);
        if (!Files.exists(old)) throw new NoSuchFileException(old.toString());
        Path target = old.resolveSibling(newName);
        if (Files.exists(target)) throw new FileAlreadyExistsException(newName);
        Files.move(old, target);
        String parent = oldPath.contains("/") ? oldPath.substring(0, oldPath.lastIndexOf('/')) : "";
        return parent.isEmpty() ? newName : parent + "/" + newName;
    }
}
