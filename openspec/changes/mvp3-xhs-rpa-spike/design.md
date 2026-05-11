## Context

MVP-2 实现了半自动发布（复制内容 + 打开平台后台）。MVP-3 的目标是引入 Playwright for Java 实现全自动发布，但小红书创作者后台是 SPA，DOM 选择器不稳定、登录态需要扫码获取。在铺开完整 RPA 架构前，先用 Spike 脚本验证核心路径。

当前后端栈：Spring Boot + Java 17 + Maven。

## Goals / Non-Goals

**Goals:**

- 验证 Playwright for Java 能否在小红书创作者后台完成：检测登录 → 上传图片 → 填标题 → 填正文 → 点发布
- 验证 browser context 持久化能否复用登录态（避免每次扫码）
- 记录 DOM 选择器，评估稳定性风险
- 产出一份"可行性评估"结论

**Non-Goals:**

- 不做正式的后端 API（无 Controller / Service）
- 不做前端对接
- 不做多平台支持（只验证小红书）
- 不做错误恢复、重试机制
- 不考虑生产部署（只在本机开发环境运行）

## Decisions

### 1. Playwright headed 模式用于首次登录

- **选择**: 使用 `launchPersistentContext()` headed 模式
- **理由**: 小红书只支持扫码登录，无法用账号密码。headed 模式弹出真实浏览器窗口让用户扫码。
- **替代方案**: 尝试读取本地 Chrome profile 的 cookies → 风险高，路径不固定，多 Chrome profile 时选错

### 2. Browser context 持久化到本地文件

- **选择**: 使用 Playwright 的 `BrowserContext.storageState()` 保存 cookies/localStorage 到 JSON 文件，存储在 `~/.aiwriter/rpa-session/xhs-state.json`
- **理由**: Playwright 原生支持，格式稳定，加载简单
- **替代方案**: 自行解析 cookie 文件 → 复杂且不必要

### 3. Spike 以 JUnit 测试类形式编写

- **选择**: 放在 `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`
- **理由**: 不污染生产代码，可以手动运行，后续验证也方便重跑
- **替代方案**: 独立 main 方法 → 不如测试类结构清晰

### 4. 图片使用本地固定文件

- **选择**: Spike 中硬编码一张测试图片路径（`src/test/resources/spike-cover.jpg`）
- **理由**: Spike 只验证路径通不通，不需要完整的图片上传 UI
- **替代方案**: 真实图片上传 UI → 属于完整架构，不在 spike 范围

## Risks / Trade-offs

- **[DOM 选择器变更]** → 小红书前端更新后选择器失效。Spike 中记录当前可用的选择器，标注稳定性等级（高/中/低）
- **[Chromium 下载体积 ~150MB]** → 首次运行需要下载。通过 Maven 插件在 `mvn test` 前自动安装
- **[登录态过期]** → cookies 有效期不确定（几天到几周）。Spike 验证检测过期并提示重新扫码的路径
- **[发布真实内容]** → Spike 可能创建真实笔记。需要用明显测试内容（标题加 `[TEST]` 前缀），并在测试后手动删除
