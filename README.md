# AI Publisher

AI Publisher 是一个面向内容创作者的本地写作与分发工作台。它把 Markdown 文稿管理、AI 助写、图片生成、平台预检和小红书半自动发布放在一个界面里，适合用来维护公众号、小红书等多平台内容草稿。

## 功能概览

- 本地 Markdown 文稿树：创建、读取、保存、重命名和删除 `.md` 文件。
- 双平台草稿：同一篇文章可维护公众号和小红书两个版本。
- AI 助写：支持大纲、起草、润色、续写等写作动作。
- AI 图片：根据标题、正文和用途生成封面/配图，并保存到本地资产目录。
- 发布前检查：针对公众号/小红书做标题、正文、敏感词等预检。
- 小红书 RPA：用 Playwright 打开创作者后台，填充标题、正文和封面图，等待人工确认后再发布。
- 本地配置持久化：模型 API Key、Base URL、模型名等配置保存到 SQLite。

## 技术栈

- Frontend: React 18, TypeScript, Vite 6, Tailwind CSS 4, Radix UI, lucide-react
- Backend: Java 21, Spring Boot 3.4, JDBC, SQLite
- AI: LangChain4j, OpenAI-compatible Chat API, Anthropic-compatible endpoint, Pollinations/OpenAI-style image API
- RPA: Playwright for Java
- Spec workflow: OpenSpec

## 项目结构

```text
.
├── backend/                  # Spring Boot API 服务
│   ├── src/main/java/com/aiwriter/
│   │   ├── ai/               # AI 网关、模型客户端、图片客户端
│   │   ├── controller/       # REST API
│   │   ├── model/            # 请求/响应 DTO
│   │   ├── rpa/              # 小红书 Playwright 自动化
│   │   └── service/          # 文稿、配置、AI、图片服务
│   └── src/main/resources/application.yml
├── frontend/                 # Vite React 应用
│   └── src/app/              # 页面、组件和 API client
├── openspec/                 # 需求变更与规格说明
└── docs/                     # 设计/实施计划文档
```

## 环境要求

- Java 21
- Maven 3.9+
- Node.js 20+ 或 22+
- pnpm 9+（也可以用 npm，但仓库当前带有 `pnpm-lock.yaml`）
- macOS/Linux/Windows 均可运行基础功能；小红书 RPA 需要可启动的本机 Chromium/Chrome 环境

## 快速启动

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

首次运行会初始化：

- 数据目录：`~/.ai-publisher`
- 文稿目录：`~/wiki/AI Writer`
- SQLite 配置库：`~/.ai-publisher/config.db`
- 图片资产目录：`~/.ai-publisher/assets`

### 2. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端默认监听 `http://localhost:5173`，并通过 Vite proxy 转发 `/api` 到 `http://localhost:8080`。

## 配置

打开应用右上角「设置」可配置写作模型和图片模型。配置会通过 `/api/v1/settings` 写入本地 SQLite。

常用配置项：

| Key | 说明 | 默认值 |
| --- | --- | --- |
| `ai_api_key` | 写作模型 API Key | 空 |
| `ai_base_url` | 写作模型 Base URL | `https://api.deepseek.com/v1` |
| `selected_model` | 写作模型名 | `deepseek-chat` |
| `image_api_key` | 图片模型 API Key | 空 |
| `image_base_url` | 图片生成 Base URL | `https://image.pollinations.ai` |
| `image_model` | 图片模型名 | `sana` |

`application.yml` 中可调整本地路径和端口：

```yaml
server:
  port: 8080

app:
  data-dir: ${user.home}/.ai-publisher
  articles-dir: ${user.home}/wiki/AI Writer
```

## 常用命令

后端：

```bash
cd backend
mvn test
mvn spring-boot:run
```

前端：

```bash
cd frontend
pnpm install
pnpm dev
pnpm build
pnpm preview
```

## API 摘要

所有接口统一返回：

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

主要接口：

- `GET /api/v1/files`：列出文稿树
- `GET /api/v1/files/content?path=...`：读取文稿内容
- `POST /api/v1/files/save`：保存文稿
- `POST /api/v1/files/create`：创建文稿
- `DELETE /api/v1/files?path=...`：删除文稿
- `POST /api/v1/files/rename`：重命名文稿
- `GET /api/v1/settings`：列出设置
- `POST /api/v1/settings`：保存设置
- `POST /api/v1/ai/generate`：AI 助写
- `POST /api/v1/ai/image`：AI 图片生成
- `POST /api/v1/ai/check`：发布前检查
- `POST /api/v1/rpa/jobs`：启动小红书 RPA 草稿准备
- `GET /api/v1/rpa/jobs/{jobId}`：查询 RPA 任务状态
- `GET /api/v1/rpa/jobs/{jobId}/logs`：读取 RPA 日志
- `POST /api/v1/rpa/jobs/{jobId}/confirm`：确认点击发布

## 小红书 RPA 注意事项

小红书发布是半自动流程：系统会打开浏览器、复用/保存登录态、填入图文内容，但不会自动越过人工确认。用户需要检查页面内容后，再在应用中确认发布。

- 登录态保存到 `~/.ai-publisher/rpa-session/xhs-state.json`
- 图文发布需要提供封面图路径
- 首次运行 Playwright 可能会下载 Chromium
- 小红书后台 DOM 可能变化，选择器失效时需要更新 `backend/src/main/java/com/aiwriter/rpa/XhsRpaPublisher.java`

## 开发说明

- 前端 API client 位于 `frontend/src/app/api.ts`。
- 后端入口是 `backend/src/main/java/com/aiwriter/AiPublisherApplication.java`。
- 需求和阶段计划在 `openspec/changes/` 下维护。
- 文稿服务只允许访问 `app.articles-dir` 内的 Markdown 文件，避免路径穿越。
- 图片资产通过 `/api/v1/assets/**` 从 `app.data-dir/assets` 读取。

## 当前阶段

仓库中已有 MVP 变更记录：

- `phase1-backend-foundation`：文件管理、配置持久化和 API 基础。
- `mvp1-real-ai-assist`：真实 AI 助写链路。
- `mvp2-publish-preflight`：发布前检查和半自动导出。
- `mvp3-xhs-rpa-spike`：小红书 RPA 技术验证与集成。
