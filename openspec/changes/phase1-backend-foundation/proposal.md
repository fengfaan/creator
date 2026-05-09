## Why

当前前端为纯静态 React 应用，所有数据（文件列表、文章内容、模型切换）均为硬编码 mock。项目需要建立 Java Spring Boot 后端，实现本地文件系统读写、配置持久化和前后端 API 联调，为后续 AI Agent 和 RPA 分发引擎提供运行基础。Phase 1 是整个系统的地基，后续所有功能都依赖它。

## What Changes

- 新建 `creator/backend/` 目录，初始化 Spring Boot 3 + Java 21 + Maven 项目骨架
- 实现文件管理 REST API：CRUD 本地 `.md` 文件（列表、读取、创建、保存、删除、重命名）
- 实现配置管理 REST API：通过 SQLite 存储和读取 API Key、模型偏好等设置项
- 前端 Vite 配置开发代理，将 `/api` 请求转发到 Spring Boot 8080 端口
- 前端 FileSidebar、EditorPanel、TopBar 等组件对接真实后端 API，替换 mock 数据
- 统一错误处理和 API 响应格式

## Capabilities

### New Capabilities
- `file-management`: 本地 .md 文件的 CRUD 操作，包括文件树遍历、内容读写、创建/删除/重命名
- `config-persistence`: 基于 SQLite 的配置持久化，管理 API Key、模型选择等应用设置
- `api-proxy`: 前端 Vite 开发代理配置，打通前后端联调链路

### Modified Capabilities
<!-- 无已有 spec，首次创建 -->

## Impact

- **新增代码**: `creator/backend/` 整个 Java 后端项目（Maven 结构）
- **修改代码**: `creator/原始ui/` 前端多个组件（FileSidebar, EditorPanel, TopBar, App）需从 mock 改为 API 调用
- **修改配置**: `creator/原始ui/vite.config.ts` 添加 API 代理规则
- **新增依赖**: Spring Boot Web, SQLite JDBC, Jackson, LangChain4j (骨架引入)
- **本地文件系统**: 首次启动创建 `~/.ai-publisher/` 目录结构（articles/, assets/, config.db）
