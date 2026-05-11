## Why

MVP-0 已经完成本地文件和配置持久化，但 AI 助写面板仍然是前端 mock。用户在真实写作流程中需要配置自己的模型服务，并用当前文章标题和正文发起真实生成请求，形成可验证的 MVP-1 闭环。

## What Changes

- 新增应用内设置弹窗，支持配置 API Key、模型和 Base URL
- 后端新增 `POST /api/v1/ai/generate`，读取设置并调用 OpenAI-compatible chat completions 接口
- 前端 AI 面板从本地 mock 改为真实请求
- 首批支持 3 个动作：生成大纲、润色、续写
- 保留统一 `ApiResponse<T>` 响应格式和现有 Vite `/api` 代理链路

## Capabilities

### New Capabilities

- `ai-assist`: 真实 AI 助写请求、动作 prompt 编排、设置驱动的模型调用

### Modified Capabilities

- `config-persistence`: 增加 AI Key、Base URL、模型配置的应用内编辑入口

## Impact

- **新增后端代码**: AI 请求/响应模型、AI service、AI controller
- **新增前端代码**: 设置弹窗组件、AI API client
- **修改前端代码**: TopBar 设置入口、EditorPanel/AIPanel 对接真实生成
- **外部依赖**: 不新增依赖，使用 Java `HttpClient` 和 Spring Boot 内置 Jackson
