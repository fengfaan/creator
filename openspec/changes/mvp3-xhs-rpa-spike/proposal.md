## Why

MVP-2 已实现半自动发布（复制内容 + 打开后台），但用户仍需手动填写标题、正文、上传图片并点击发布。MVP-3 计划引入 RPA 自动化，但在铺开完整架构前，需要先用最小脚本验证小红书创作者后台的自动发布路径在技术上是否可行。

## What Changes

- 后端新增 Playwright for Java 依赖
- 编写独立 Spike 测试类，覆盖小红书发布核心路径：登录态检测 → 上传图片 → 填标题 → 填正文 → 点发布
- 验证 Playwright browser context 持久化（cookies/localStorage）用于复用登录态
- 记录 DOM 选择器稳定性评估结果

## Capabilities

### New Capabilities

- `xhs-rpa-spike`: 用 Playwright for Java 编写最小验证脚本，确认小红书自动发布路径的技术可行性

### Modified Capabilities

_(无现有 spec 需要修改)_

## Impact

- **新增依赖**: `com.microsoft.playwright:playwright` (首次运行下载 Chromium ~150MB)
- **新增测试代码**: `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`
- **不修改任何现有代码**: Spike 是独立测试类，不影响生产代码
- **本地磁盘**: browser context 持久化文件存储在项目目录外（如 `~/.aiwriter/rpa-session/`）
