## Context

当前项目是一个个人智能写作与分发工具，前端已有基于 React + Tailwind CSS 的 UI 原型（`creator/原始ui/`），包含编辑器、预览面板、文件侧边栏等组件，但所有数据均为客户端 mock。后端从零开始建设。

技术栈已确定：
- 前端：React + Vite + Tailwind CSS（保持现有，不改 Vue）
- 后端：Java 21 + Spring Boot 3 + Maven
- 数据：SQLite（配置）+ java.nio.file（.md 文件）
- 后端代码位于 `creator/backend/`
- 开发模式：Vite 5173 + Spring Boot 8080，Vite proxy 转发 API

## Goals / Non-Goals

**Goals:**
- 建立可运行的 Spring Boot 项目骨架，包含标准分层结构（Controller / Service / Repository）
- 实现文件 CRUD API，支持本地 .md 文件的真实读写
- 实现 SQLite 配置持久化，支持 API Key 等设置项的存取
- 前端通过 Vite proxy 对接后端，替换所有 mock 数据
- 首次启动自动创建 `~/.ai-publisher/` 数据目录

**Non-Goals:**
- AI Agent 功能（Phase 2）
- RPA 分发引擎（Phase 3）
- 用户认证/权限系统（个人工具，单用户）
- 移动端适配（Phase 4）
- 图片上传/管理（后续）

## Decisions

### 1. 后端项目结构

采用标准 Spring Boot 分层架构：

```
backend/
├── pom.xml
├── src/main/java/com/aiwriter/
│   ├── AiPublisherApplication.java
│   ├── controller/
│   │   ├── FileController.java
│   │   └── ConfigController.java
│   ├── service/
│   │   ├── FileService.java
│   │   └── ConfigService.java
│   ├── model/
│   │   ├── FileInfo.java
│   │   ├── ApiResponse.java
│   │   └── ConfigItem.java
│   └── config/
│       ├── WebConfig.java (CORS)
│       └── DataInitConfig.java (首次启动初始化)
└── src/main/resources/
    └── application.yml
```

**替代方案**: Flat 结构（不分层）— 放弃，因为后续 AI 和 RPA 模块会快速膨胀。

### 2. 文件管理：直接操作 java.nio.file

不引入虚拟文件系统或数据库存储文章。直接读写 `~/.ai-publisher/articles/` 下的 .md 文件。

**理由**: PRD 明确"本地优先，数据以 Markdown 文件形式存储"，直接文件操作最简单且用户可直接访问文件。

### 3. 配置存储：SQLite + JDBC

使用 Spring Boot 的 JDBC 支持直接操作 SQLite，不引入 JPA/Hibernate。

**理由**: 配置数据量极小（几条 API Key），JPA 过重。SQLite 单文件、零配置、嵌入应用。使用 sqlite-jdbc 驱动。

### 4. API 响应格式

统一包装为 `ApiResponse<T>`:
```json
{ "code": 200, "message": "ok", "data": T }
```

错误时：
```json
{ "code": 404, "message": "File not found", "data": null }
```

### 5. Vite Proxy 配置

在 `vite.config.ts` 中配置：
```ts
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/ws': { target: 'http://localhost:8080', ws: true }
  }
}
```

### 6. Maven 依赖（Phase 1 最小集）

- `spring-boot-starter-web`
- `spring-boot-starter-jdbc`
- `sqlite-jdbc`
- `jackson-databind`（Spring Boot 自带）
- `lombok`（减少样板代码）

LangChain4j 和 Playwright 在后续 Phase 引入。

## Risks / Trade-offs

- **[文件并发安全]** → Phase 1 为单用户本地工具，不加锁。后续如需多端访问再引入文件锁。
- **[SQLite 驱动兼容性]** → 使用 xerial/sqlite-jdbc，在 macOS 上验证通过。Spring Boot 3 + Java 21 兼容。
- **[前后端联调延迟]** → Vite proxy 配置简单，但需确保 Spring Boot CORS 和 proxy 不冲突。优先依赖 proxy，不额外配 CORS。
