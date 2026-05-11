## 1. 后端真实 AI API

- [x] 1.1 创建 AI generate 请求/响应模型
- [x] 1.2 实现 `AiWritingService`：读取设置、构造 prompt、调用底层 AI client
- [x] 1.3 实现 `AiController`：`POST /api/v1/ai/generate`
- [x] 1.4 添加基础错误处理和最小测试
- [x] 1.5 将底层 AI 能力封装到 `com.aiwriter.ai` 模块，业务 Controller/Service/Model 保持在常规业务分层

## 2. 前端设置页

- [x] 2.1 新增设置弹窗组件，支持 API Key、模型、Base URL
- [x] 2.2 TopBar 设置按钮打开应用内弹窗
- [x] 2.3 模型快捷切换与自定义模型值兼容

## 3. 前端 AI 面板真实请求

- [x] 3.1 API client 新增 `api.ai.generate`
- [x] 3.2 AI 面板移除 mock 生成，调用后端真实接口
- [x] 3.3 仅保留生成大纲、润色、续写三个动作
- [x] 3.4 将当前标题和正文传入 AI 请求

## 4. 验证

- [x] 4.1 `mvn test`
- [x] 4.2 `npm run build`
- [x] 4.3 本地启动后端并验证未配置 API Key 时返回可读错误
