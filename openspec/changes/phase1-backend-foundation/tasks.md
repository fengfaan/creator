## 1. Spring Boot 项目骨架

- [ ] 1.1 在 `creator/backend/` 下初始化 Maven 项目，配置 pom.xml（Java 21, Spring Boot 3, sqlite-jdbc, lombok）
- [ ] 1.2 创建主启动类 `AiPublisherApplication.java`
- [ ] 1.3 配置 `application.yml`（端口 8080, 数据目录 `~/.ai-publisher/`）
- [ ] 1.4 创建 `DataInitConfig.java`，首次启动自动创建数据目录和 SQLite 表

## 2. 后端文件管理 API

- [ ] 2.1 创建 `FileInfo` 和 `ApiResponse` 模型类
- [ ] 2.2 实现 `FileService`：文件树遍历、内容读写、创建/删除/重命名，含路径安全校验
- [ ] 2.3 实现 `FileController`：`GET /api/v1/files`、`GET /api/v1/files/content`、`POST /api/v1/files/save`、`POST /api/v1/files/create`、`DELETE /api/v1/files`、`POST /api/v1/files/rename`

## 3. 后端配置管理 API

- [ ] 3.1 创建 `ConfigItem` 模型类
- [ ] 3.2 实现 `ConfigService`：基于 Spring JDBC 的 SQLite 配置读写
- [ ] 3.3 实现 `ConfigController`：`GET /api/v1/settings`、`GET /api/v1/settings/{key}`、`POST /api/v1/settings`

## 4. 前端 API 对接

- [ ] 4.1 创建前端 API 客户端模块 `src/app/api.ts`，封装 fetch 调用和错误处理
- [ ] 4.2 配置 Vite proxy：`vite.config.ts` 添加 `/api` 和 `/ws` 代理到 8080
- [ ] 4.3 改造 `FileSidebar` 组件：从 `GET /api/v1/files` 加载文件树，选中文件时触发回调加载内容
- [ ] 4.4 改造 `App.tsx`：文件选中后调用 `GET /api/v1/files/content` 加载内容，编辑后 debounce 调用 `POST /api/v1/files/save` 保存
- [ ] 4.5 改造 `TopBar` 模型切换：通过 `POST /api/v1/settings` 持久化选中的模型
- [ ] 4.6 添加"新建文档"功能：调用 `POST /api/v1/files/create` 并刷新文件树

## 5. 验证与调试

- [ ] 5.1 确认 Spring Boot 启动成功，数据目录自动创建
- [ ] 5.2 确认前后端联调通过：文件列表加载、内容读写、模型切换持久化均正常工作
