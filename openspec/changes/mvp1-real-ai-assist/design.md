## Context

当前应用是本地优先的写作工具。配置已经存入 SQLite，前端通过 `/api` 代理访问 Spring Boot 后端。MVP-1 的目标不是构建完整 Agent，而是把 AI 助写从 mock 变为可用的真实生成链路。

## Goals / Non-Goals

**Goals:**
- 设置页可维护 `ai_api_key`、`ai_base_url`、`selected_model`
- 后端提供 `POST /api/v1/ai/generate`
- AI 面板支持生成大纲、润色、续写三个动作
- 后端返回清晰错误，例如未配置 API Key 或上游模型返回异常

**Non-Goals:**
- 流式输出
- 多供应商专用 SDK
- 多轮会话记忆
- 图片生成、发布 Agent、RPA 分发

## Decisions

### 0. AI 模块边界

服务端 AI 底层能力统一放在 `com.aiwriter.ai` 模块下，业务入口仍走常规 `controller/model/service` 分层：

- `controller.AiController`: HTTP API 边界，处理 `/api/v1/ai/generate`
- `model.AiGenerateRequest/Response`: 业务请求/响应 DTO
- `service.AiWritingService`: 读取业务配置、校验动作、组装写作 prompt
- `ai.AiClient`: 底层 AI 能力抽象
- `ai.OpenAiCompatibleAiClient`: OpenAI-compatible Chat Completions 实现

**理由**: 业务必须经 Controller 处理，AI 模块只封装模型调用能力。上游模型协议、HTTP 调用、响应解析等底层细节与具体写作业务隔离，后续替换供应商或扩展 Agent 时不污染 Controller 和业务模型。

### 1. 使用 OpenAI-compatible HTTP 调用

后端使用 Java `HttpClient` 直接请求 `{baseUrl}/chat/completions`。如果用户填写的 Base URL 已经以 `/chat/completions` 结尾，则原样使用；否则自动拼接。

**理由**: DeepSeek、OpenAI-compatible 网关和许多本地模型服务都支持该协议，MVP-1 不引入额外 SDK，降低依赖风险。

### 2. 设置键名

- `ai_api_key`
- `ai_base_url`
- `selected_model`

`selected_model` 继续沿用 MVP-0 顶部栏模型偏好的键名，避免重复配置。

### 3. 生成请求结构

前端发送：

```json
{
  "action": "outline|polish|continue",
  "title": "文章标题",
  "content": "当前正文"
}
```

后端业务 service 按 action 构造 system/user prompt，通过底层 `AiClient` 调用模型，并返回：

```json
{
  "text": "生成内容",
  "model": "deepseek-chat"
}
```

## Risks / Trade-offs

- **[无流式输出]** → MVP-1 先确保真实请求闭环，流式可作为后续增强。
- **[不同供应商模型 ID 不一致]** → 设置页允许自由输入模型 ID，顶部栏只提供常用快捷选项。
- **[API Key 存储在本地 SQLite]** → 当前为个人本地工具，符合 MVP 范围；后续可增加系统钥匙串或加密。
