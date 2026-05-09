# Phase 1 Backend Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a Spring Boot 3 backend with file CRUD and config persistence APIs, then connect the existing React frontend to use real backend data instead of mocks.

**Architecture:** Spring Boot 3 (Java 21, Maven) serves REST APIs at `localhost:8080`. Frontend (React + Vite) runs at `localhost:5173` with a proxy forwarding `/api` and `/ws` to the backend. Files are stored as `.md` on disk under `~/.ai-publisher/articles/`. Config is stored in SQLite at `~/.ai-publisher/config.db`.

**Tech Stack:** Java 21, Spring Boot 3.4, Maven, sqlite-jdbc, Lombok / React 18, Vite 6, Tailwind CSS 4

---

## File Structure

### Backend (create all)

```
creator/backend/
├── pom.xml
├── src/main/java/com/aiwriter/
│   ├── AiPublisherApplication.java          — Spring Boot main class
│   ├── config/
│   │   └── DataInitializer.java             — Creates ~/.ai-publisher/ dirs + SQLite schema on startup
│   ├── model/
│   │   ├── ApiResponse.java                 — Generic wrapper {code, message, data}
│   │   ├── FileInfo.java                    — File tree node {id, name, path, type, children}
│   │   └── ConfigItem.java                  — Config entry {key, value, updatedAt}
│   ├── service/
│   │   ├── FileService.java                 — File CRUD via java.nio.file
│   │   └── ConfigService.java              — Config CRUD via JdbcTemplate + SQLite
│   └── controller/
│       ├── FileController.java              — REST endpoints for /api/v1/files/*
│       └── ConfigController.java            — REST endpoints for /api/v1/settings/*
└── src/main/resources/
    └── application.yml                      — Port, data-dir path, SQLite config
```

### Frontend (modify existing)

```
creator/frontend/
├── vite.config.ts                           — ADD proxy config
├── src/app/api.ts                           — CREATE: centralized API client
├── src/app/App.tsx                          — MODIFY: load files from API, debounce save
├── src/app/components/file-sidebar.tsx      — MODIFY: fetch file tree from API
└── src/app/components/top-bar.tsx           — MODIFY: persist model to API
```

---

## Task 1: Maven Project Skeleton

**Files:**
- Create: `creator/backend/pom.xml`
- Create: `creator/backend/src/main/java/com/aiwriter/AiPublisherApplication.java`
- Create: `creator/backend/src/main/resources/application.yml`

- [ ] **Step 1: Create Maven pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
        <relativePath/>
    </parent>
    <groupId>com.aiwriter</groupId>
    <artifactId>ai-publisher</artifactId>
    <version>0.0.1</version>
    <name>AI Publisher</name>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.47.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

```java
package com.aiwriter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiPublisherApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiPublisherApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

```yaml
server:
  port: 8080

app:
  data-dir: ${user.home}/.ai-publisher

spring:
  datasource:
    url: jdbc:sqlite:${app.data-dir}/config.db
    driver-class-name: org.sqlite.JDBC
```

- [ ] **Step 4: Verify Maven build succeeds**

Run: `cd /Users/fengfan/creator/backend && mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat: init Spring Boot 3 backend skeleton"
```

---

## Task 2: Data Initialization + Model Classes

**Files:**
- Create: `creator/backend/src/main/java/com/aiwriter/config/DataInitializer.java`
- Create: `creator/backend/src/main/java/com/aiwriter/model/ApiResponse.java`
- Create: `creator/backend/src/main/java/com/aiwriter/model/FileInfo.java`
- Create: `creator/backend/src/main/java/com/aiwriter/model/ConfigItem.java`

- [ ] **Step 1: Create DataInitializer**

```java
package com.aiwriter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    @Value("${app.data-dir}")
    private String dataDir;

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void init() throws Exception {
        Path root = Path.of(dataDir);
        Files.createDirectories(root.resolve("articles"));
        Files.createDirectories(root.resolve("assets"));

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """);
        log.info("Data directory initialized at {}", root.toAbsolutePath());
    }
}
```

- [ ] **Step 2: Create ApiResponse model**

```java
package com.aiwriter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "ok", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 3: Create FileInfo model**

```java
package com.aiwriter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String id;
    private String name;
    private String path;
    private String type; // "file" or "folder"
    private List<FileInfo> children;
}
```

- [ ] **Step 4: Create ConfigItem model**

```java
package com.aiwriter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItem {
    private String key;
    private String value;
    private String updatedAt;
}
```

- [ ] **Step 5: Verify build**

Run: `cd /Users/fengfan/creator/backend && mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/aiwriter/config/ backend/src/main/java/com/aiwriter/model/
git commit -m "feat: add data initializer and model classes"
```

---

## Task 3: FileService — File CRUD Logic

**Files:**
- Create: `creator/backend/src/main/java/com/aiwriter/service/FileService.java`

- [ ] **Step 1: Create FileService**

```java
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
            stream.sorted().forEach(p -> children.add(buildNode(p, "")));
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
                stream.sorted().forEach(p -> children.add(buildNode(p, rel)));
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
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/fengfan/creator/backend && mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/aiwriter/service/FileService.java
git commit -m "feat: add FileService with CRUD and path traversal protection"
```

---

## Task 4: FileController — REST Endpoints

**Files:**
- Create: `creator/backend/src/main/java/com/aiwriter/controller/FileController.java`

- [ ] **Step 1: Create FileController**

```java
package com.aiwriter.controller;

import com.aiwriter.model.ApiResponse;
import com.aiwriter.model.FileInfo;
import com.aiwriter.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    public ApiResponse<List<FileInfo>> list() throws Exception {
        return ApiResponse.ok(fileService.listTree());
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<String>> readContent(@RequestParam String path) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(fileService.readContent(path)));
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(403, "Access denied"));
            }
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
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(409, "File already exists"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam String path) {
        try {
            fileService.deleteFile(path);
            return ResponseEntity.ok(ApiResponse.ok());
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
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(409, "A file with that name already exists"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "File not found"));
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/fengfan/creator/backend && mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/aiwriter/controller/FileController.java
git commit -m "feat: add FileController with REST endpoints for file CRUD"
```

---

## Task 5: ConfigService + ConfigController

**Files:**
- Create: `creator/backend/src/main/java/com/aiwriter/service/ConfigService.java`
- Create: `creator/backend/src/main/java/com/aiwriter/controller/ConfigController.java`

- [ ] **Step 1: Create ConfigService**

```java
package com.aiwriter.service;

import com.aiwriter.model.ConfigItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final JdbcTemplate jdbc;

    public ConfigItem get(String key) {
        List<ConfigItem> results = jdbc.query(
                "SELECT key, value, updated_at FROM settings WHERE key = ?",
                (rs, i) -> new ConfigItem(rs.getString("key"), rs.getString("value"), rs.getString("updated_at")),
                key
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public List<ConfigItem> list() {
        return jdbc.query(
                "SELECT key, value, updated_at FROM settings ORDER BY key",
                (rs, i) -> new ConfigItem(rs.getString("key"), rs.getString("value"), rs.getString("updated_at"))
        );
    }

    public void set(String key, String value) {
        jdbc.update("""
            INSERT INTO settings (key, value, updated_at) VALUES (?, ?, datetime('now'))
            ON CONFLICT(key) DO UPDATE SET value = ?, updated_at = datetime('now')
        """, key, value, value);
    }
}
```

- [ ] **Step 2: Create ConfigController**

```java
package com.aiwriter.controller;

import com.aiwriter.model.ApiResponse;
import com.aiwriter.model.ConfigItem;
import com.aiwriter.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public ApiResponse<List<ConfigItem>> list() {
        return ApiResponse.ok(configService.list());
    }

    @GetMapping("/{key}")
    public ApiResponse<ConfigItem> get(@PathVariable String key) {
        return ApiResponse.ok(configService.get(key));
    }

    @PostMapping
    public ApiResponse<Void> set(@RequestBody Map<String, String> body) {
        configService.set(body.get("key"), body.get("value"));
        return ApiResponse.ok();
    }
}
```

- [ ] **Step 3: Verify build**

Run: `cd /Users/fengfan/creator/backend && mvn compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/aiwriter/service/ConfigService.java backend/src/main/java/com/aiwriter/controller/ConfigController.java
git commit -m "feat: add ConfigService and ConfigController for SQLite settings"
```

---

## Task 6: Start Backend and Smoke Test

**Files:** None (verification only)

- [ ] **Step 1: Start the Spring Boot application**

Run: `cd /Users/fengfan/creator/backend && mvn spring-boot:run`
Expected: Application starts on port 8080, log shows `Data directory initialized at /Users/fengfan/.ai-publisher`

- [ ] **Step 2: Verify data directory was created**

Run: `ls -la ~/.ai-publisher/`
Expected: `articles/`, `assets/`, `config.db` present

- [ ] **Step 3: Test file list endpoint**

Run: `curl -s http://localhost:8080/api/v1/files | python3 -m json.tool`
Expected: `{"code":200,"message":"ok","data":[{"id":"root","name":"我的文稿","path":"","type":"folder","children":[]}]}`

- [ ] **Step 4: Test file create and read**

Run:
```bash
curl -s -X POST http://localhost:8080/api/v1/files/create -H 'Content-Type: application/json' -d '{"name":"test.md"}'
curl -s 'http://localhost:8080/api/v1/files/content?path=test.md'
curl -s -X POST http://localhost:8080/api/v1/files/save -H 'Content-Type: application/json' -d '{"path":"test.md","content":"# Hello"}'
curl -s 'http://localhost:8080/api/v1/files/content?path=test.md'
```
Expected: Create returns `test.md`, read returns empty then `# Hello`

- [ ] **Step 5: Test config endpoints**

Run:
```bash
curl -s -X POST http://localhost:8080/api/v1/settings -H 'Content-Type: application/json' -d '{"key":"selected_model","value":"DeepSeek-V3"}'
curl -s http://localhost:8080/api/v1/settings/selected_model
```
Expected: Get returns `{"code":200,"message":"ok","data":{"key":"selected_model","value":"DeepSeek-V3",...}}`

- [ ] **Step 6: Stop the server**

Press `Ctrl+C` in the terminal running `mvn spring-boot:run`

---

## Task 7: Vite Proxy + Frontend API Client

**Files:**
- Modify: `creator/frontend/vite.config.ts`
- Create: `creator/frontend/src/app/api.ts`

- [ ] **Step 1: Update vite.config.ts with proxy**

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      "/ws": { target: "http://localhost:8080", ws: true },
    },
  },
});
```

- [ ] **Step 2: Create frontend API client module**

```typescript
// creator/frontend/src/app/api.ts

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const body: ApiResponse<T> = await res.json();
  if (body.code !== 200 && body.code !== 201) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}

export interface FileNode {
  id: string;
  name: string;
  path: string;
  type: "file" | "folder";
  children?: FileNode[];
}

export const api = {
  files: {
    list: () => request<FileNode[]>("/api/v1/files"),
    readContent: (path: string) =>
      request<string>(`/api/v1/files/content?path=${encodeURIComponent(path)}`),
    save: (path: string, content: string) =>
      request<string>("/api/v1/files/save", {
        method: "POST",
        body: JSON.stringify({ path, content }),
      }),
    create: (name: string, folder?: string) =>
      request<string>("/api/v1/files/create", {
        method: "POST",
        body: JSON.stringify({ name, folder: folder || "" }),
      }),
    delete: (path: string) =>
      request<void>(`/api/v1/files?path=${encodeURIComponent(path)}`, { method: "DELETE" }),
    rename: (oldPath: string, newName: string) =>
      request<string>("/api/v1/files/rename", {
        method: "POST",
        body: JSON.stringify({ oldPath, newName }),
      }),
  },
  settings: {
    list: () => request<{ key: string; value: string }[]>("/api/v1/settings"),
    get: (key: string) =>
      request<{ key: string; value: string } | null>(`/api/v1/settings/${key}`),
    set: (key: string, value: string) =>
      request<void>("/api/v1/settings", {
        method: "POST",
        body: JSON.stringify({ key, value }),
      }),
  },
};
```

- [ ] **Step 3: Verify frontend builds**

Run: `cd /Users/fengfan/creator/frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 4: Commit**

```bash
git add frontend/vite.config.ts frontend/src/app/api.ts
git commit -m "feat: add Vite proxy config and frontend API client module"
```

---

## Task 8: Connect FileSidebar to Backend

**Files:**
- Modify: `creator/frontend/src/app/components/file-sidebar.tsx`

- [ ] **Step 1: Rewrite FileSidebar to fetch from API**

Replace the entire content of `creator/frontend/src/app/components/file-sidebar.tsx` with:

```tsx
import { Search, Plus, FileText, Folder, FolderOpen, ChevronRight, ChevronDown } from "lucide-react";
import { useState, useEffect, useCallback } from "react";
import { api, FileNode } from "../api";

interface Props {
  activeId: string;
  activePath: string;
  onSelect: (id: string, name: string, path: string) => void;
  onFilesChanged?: () => void;
}

export function FileSidebar({ activeId, activePath, onSelect, onFilesChanged }: Props) {
  const [query, setQuery] = useState("");
  const [tree, setTree] = useState<FileNode[]>([]);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ root: true });

  const loadTree = useCallback(async () => {
    try {
      const data = await api.files.list();
      setTree(data);
    } catch (e) {
      console.error("Failed to load file tree:", e);
    }
  }, []);

  useEffect(() => { loadTree(); }, [loadTree]);

  const handleCreate = async () => {
    const name = prompt("输入文件名（例如：新文章.md）");
    if (!name) return;
    try {
      await api.files.create(name);
      await loadTree();
      onFilesChanged?.();
    } catch (e) {
      console.error("Failed to create file:", e);
    }
  };

  const toggleExpand = (id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const matches = (node: FileNode): boolean =>
    !query || node.name.toLowerCase().includes(query.toLowerCase());

  const renderNode = (node: FileNode, depth: number) => {
    const isExpanded = expanded[node.id];
    const isActive = activeId === node.id || activePath === node.path;

    if (node.type === "folder") {
      return (
        <div key={node.id}>
          <button
            onClick={() => toggleExpand(node.id)}
            aria-label={`${isExpanded ? "折叠" : "展开"} ${node.name}`}
            className="flex items-center w-full h-9 gap-1 rounded transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
            style={{ paddingLeft: 8 + depth * 14, paddingRight: 8 }}
          >
            {isExpanded ? (
              <ChevronDown size={12} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} />
            ) : (
              <ChevronRight size={12} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} />
            )}
            {isExpanded ? (
              <FolderOpen size={14} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            ) : (
              <Folder size={14} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            )}
            <span style={{ fontSize: 13, color: "var(--text-primary)" }}>{node.name}</span>
          </button>
          {isExpanded && node.children?.map((c) => renderNode(c, depth + 1))}
        </div>
      );
    }

    if (!matches(node)) return null;

    return (
      <button
        key={node.id}
        onClick={() => onSelect(node.id, node.name, node.path)}
        className="flex items-center w-full h-9 gap-1.5 transition-colors relative hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
        style={{
          paddingLeft: 8 + depth * 14 + 12,
          paddingRight: 8,
          background: isActive ? "var(--accent-primary-light)" : undefined,
        }}
      >
        {isActive && (
          <span
            className="absolute left-0 top-1 bottom-1 w-[3px] rounded-r"
            style={{ background: "var(--accent-primary)" }}
          />
        )}
        <FileText size={13} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
        <span
          style={{
            fontSize: 13,
            color: isActive ? "var(--accent-primary)" : "var(--text-primary)",
            fontWeight: isActive ? 500 : 400,
          }}
          className="truncate"
        >
          {node.name}
        </span>
      </button>
    );
  };

  return (
    <div
      className="flex flex-col h-full shrink-0"
      style={{ width: 240, background: "var(--bg-surface)", borderRight: "1px solid var(--border-default)" }}
    >
      <div className="p-3 space-y-2">
        <label className="flex items-center h-9 px-2.5 gap-2 rounded-md" style={{ background: "var(--bg-deepest)", border: "1px solid var(--border-subtle)" }}>
          <Search size={14} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} aria-hidden="true" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="搜索文件..."
            aria-label="搜索文件"
            className="flex-1 bg-transparent outline-none"
            style={{ fontSize: 13, color: "var(--text-primary)" }}
          />
        </label>
        <button
          onClick={handleCreate}
          className="flex items-center justify-center w-full h-9 gap-1.5 rounded-md transition-colors hover:opacity-90 active:scale-[0.98]"
          style={{
            background: "var(--accent-primary-light)",
            color: "var(--accent-primary)",
            fontSize: 13,
            fontWeight: 500,
          }}
        >
          <Plus size={14} strokeWidth={1.5} />
          新建文档
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-2 pb-4">
        {tree.map((n) => renderNode(n, 0))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/fengfan/creator/frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/components/file-sidebar.tsx
git commit -m "feat: connect FileSidebar to backend API"
```

---

## Task 9: Connect App.tsx to Backend

**Files:**
- Modify: `creator/frontend/src/app/App.tsx`

- [ ] **Step 1: Rewrite App.tsx to use backend APIs**

Replace the entire content of `creator/frontend/src/app/App.tsx` with:

```tsx
import { useEffect, useRef, useState, useMemo, useCallback } from "react";
import { TopBar } from "./components/top-bar";
import { FileSidebar } from "./components/file-sidebar";
import { EditorPanel } from "./components/editor-panel";
import { PreviewPanel } from "./components/preview-panel";
import { PublishModal } from "./components/publish-modal";
import { api } from "./api";

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isDark, setIsDark] = useState(false);
  const [fileName, setFileName] = useState("");
  const [filePath, setFilePath] = useState("");
  const [activeId, setActiveId] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [leftPct, setLeftPct] = useState(50);
  const [publishOpen, setPublishOpen] = useState(false);
  const [savedContent, setSavedContent] = useState("");
  const dragRef = useRef<{ dragging: boolean; startX: number; startPct: number }>({
    dragging: false,
    startX: 0,
    startPct: 50,
  });
  const containerRef = useRef<HTMLDivElement>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!dragRef.current.dragging || !containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const dx = e.clientX - dragRef.current.startX;
      const newPct = dragRef.current.startPct + (dx / rect.width) * 100;
      setLeftPct(Math.max(30, Math.min(75, newPct)));
    };
    const onUp = () => {
      dragRef.current.dragging = false;
      document.body.style.cursor = "";
      document.querySelectorAll(".resize-divider").forEach((el) => el.classList.remove("dragging"));
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
    return () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
    };
  }, []);

  const startDrag = (e: React.MouseEvent) => {
    dragRef.current = { dragging: true, startX: e.clientX, startPct: leftPct };
    document.body.style.cursor = "col-resize";
    (e.currentTarget as HTMLElement).classList.add("dragging");
  };

  const loadFile = useCallback(async (id: string, name: string, path: string) => {
    setActiveId(id);
    setFileName(name);
    setFilePath(path);
    try {
      const text = await api.files.readContent(path);
      setContent(text);
      setSavedContent(text);
      const firstLine = text.split("\n").find((l) => l.trim() && !l.trim().startsWith("#"));
      const heading = text.match(/^#\s+(.+)/m);
      setTitle(heading ? heading[1] : name.replace(/\.md$/, ""));
    } catch {
      setContent("");
      setTitle(name.replace(/\.md$/, ""));
    }
  }, []);

  const handleContentChange = useCallback((newContent: string) => {
    setContent(newContent);
    if (!filePath) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(async () => {
      try {
        await api.files.save(filePath, newContent);
        setSavedContent(newContent);
      } catch (e) {
        console.error("Auto-save failed:", e);
      }
    }, 1000);
  }, [filePath]);

  const wordCount = useMemo(() => content.replace(/\s/g, "").length, [content]);
  const lineCount = useMemo(() => content.split("\n").length, [content]);
  const isDirty = content !== savedContent;

  return (
    <div className={isDark ? "dark" : ""}>
      <a href="#main-editor" className="skip-link">跳转到编辑器</a>
      <div
        className="flex flex-col h-screen w-full overflow-hidden"
        style={{ background: "var(--bg-deepest)", color: "var(--text-primary)" }}
      >
        <TopBar
          fileName={fileName}
          onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          isDark={isDark}
          onToggleTheme={() => setIsDark(!isDark)}
          onPublish={() => setPublishOpen(true)}
        />

        <div className="flex flex-1 min-h-0">
          <div className={`sidebar-wrap${sidebarOpen ? "" : " collapsed"}`} style={{ width: 240 }}>
            <FileSidebar
              activeId={activeId}
              activePath={filePath}
              onSelect={loadFile}
            />
          </div>

          <div id="main-editor" ref={containerRef} className="flex-1 flex min-w-0 min-h-0" tabIndex={-1}>
            <div style={{ width: `${leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <EditorPanel title={title} setTitle={setTitle} content={content} setContent={handleContentChange} />
            </div>
            <div className="resize-divider" onMouseDown={startDrag} onDoubleClick={() => setLeftPct(50)} />
            <div style={{ width: `${100 - leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <PreviewPanel title={title} content={content} />
            </div>
          </div>
        </div>

        {/* Status bar */}
        <div
          className="flex items-center justify-between h-7 px-4 shrink-0"
          role="status"
          style={{
            background: "var(--bg-deepest)",
            borderTop: "1px solid var(--border-subtle)",
            fontSize: 12,
            color: "var(--text-secondary)",
            fontFamily: "var(--font-mono)",
          }}
        >
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <span
                className="inline-block w-1.5 h-1.5 rounded-full"
                style={{ background: isDirty ? "var(--status-warning)" : "var(--status-success)" }}
                aria-hidden="true"
              />
              {isDirty ? "未保存" : "已自动保存"}
            </span>
            <span>UTF-8</span>
            <span>Markdown</span>
          </div>
          <div className="flex items-center gap-3">
            <span>{wordCount} 字</span>
            <span>{lineCount} 行</span>
            <span>Ln {lineCount}, Col 1</span>
          </div>
        </div>

        <PublishModal open={publishOpen} onClose={() => setPublishOpen(false)} title={title} />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/fengfan/creator/frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/App.tsx
git commit -m "feat: connect App.tsx to backend API with debounce auto-save"
```

---

## Task 10: Connect TopBar Model Selection to Backend

**Files:**
- Modify: `creator/frontend/src/app/components/top-bar.tsx`

- [ ] **Step 1: Update TopBar to persist model via API**

In `creator/frontend/src/app/components/top-bar.tsx`:

Add the import at the top (after existing imports):

```typescript
import { api } from "../api";
```

Replace the `MODELS` constant and the component's state initialization:

```typescript
const MODELS = ["DeepSeek-V3", "Claude Sonnet 4.6", "Qwen-Max"];

export function TopBar({ fileName, onToggleSidebar, isDark, onToggleTheme, onPublish }: TopBarProps) {
  const [model, setModel] = useState(MODELS[0]);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [activeIdx, setActiveIdx] = useState(0);

  useEffect(() => {
    api.settings.get("selected_model").then((item) => {
      if (item?.value && MODELS.includes(item.value)) {
        setModel(item.value);
      }
    }).catch(() => {});
  }, []);
```

Replace the model selection handler inside `handleDropdownKey` and the dropdown button's `onClick`:

Find this line:
```typescript
if (e.key === "Enter") { setModel(MODELS[activeIdx]); setOpen(false); }
```
Replace with:
```typescript
if (e.key === "Enter") {
  const m = MODELS[activeIdx];
  setModel(m);
  setOpen(false);
  api.settings.set("selected_model", m).catch(() => {});
}
```

Find this line in the dropdown trigger button:
```typescript
onClick={() => { setOpen(!open); setActiveIdx(MODELS.indexOf(model)); }}
```
Keep it as is (it just opens dropdown).

Find these lines in the dropdown option buttons:
```typescript
onClick={() => { setModel(m); setOpen(false); }}
```
Replace with:
```typescript
onClick={() => { setModel(m); setOpen(false); api.settings.set("selected_model", m).catch(() => {}); }}
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/fengfan/creator/frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/components/top-bar.tsx
git commit -m "feat: persist model selection to backend via settings API"
```

---

## Task 11: End-to-End Integration Test

**Files:** None (verification only)

- [ ] **Step 1: Start the backend**

Run: `cd /Users/fengfan/creator/backend && mvn spring-boot:run &`
Expected: Application starts on port 8080

- [ ] **Step 2: Start the frontend dev server**

Run: `cd /Users/fengfan/creator/frontend && pnpm dev &`
Expected: Vite starts on port 5173

- [ ] **Step 3: Open browser and verify**

Open: `http://localhost:5173`
Check:
1. File sidebar shows "我的文稿" folder (empty)
2. Click "新建文档", enter a name → file appears in sidebar
3. Click the new file → editor loads (empty)
4. Type content → wait 1s → status bar shows "已自动保存"
5. Reload page → content persists
6. Switch model in TopBar → reload → model persists

- [ ] **Step 4: Stop both servers**

Run: `kill %1 %2 2>/dev/null; pkill -f "spring-boot" 2>/dev/null; true`

- [ ] **Step 5: Final commit (if any fixes needed)**

```bash
git add -A
git commit -m "fix: address integration issues from e2e testing"
```
