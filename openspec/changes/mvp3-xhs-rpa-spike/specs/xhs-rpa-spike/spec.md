## ADDED Requirements

### Requirement: Playwright for Java 集成验证
Spike 脚本 SHALL 使用 Playwright for Java 启动 Chromium 浏览器，并成功打开小红书创作者后台发布页面。

#### Scenario: 首次运行无保存 session
- **WHEN** 本地不存在 `~/.aiwriter/rpa-session/xhs-state.json`
- **THEN** Spike 以 headed 模式启动 Chromium，弹出浏览器窗口等待用户扫码登录，登录成功后保存 session 到 JSON 文件

#### Scenario: 后续运行复用 session
- **WHEN** 本地存在有效的 `xhs-state.json`
- **THEN** Spike 加载已保存的 session，直接进入发布页面，不弹出登录窗口

#### Scenario: session 已过期
- **WHEN** 加载已保存的 session 后，页面重定向到登录页
- **THEN** Spike 检测到重定向，日志输出"登录态已过期"，重新弹出浏览器窗口让用户扫码

### Requirement: 小红书发布核心路径验证
Spike 脚本 SHALL 在已登录状态下，完成以下步骤：上传封面图片 → 填写标题 → 填写正文 → 点击发布按钮。

#### Scenario: 完整发布路径执行成功
- **WHEN** 用户已登录且 session 有效
- **THEN** Spike 依次执行：上传测试图片、填写标题（带 `[TEST]` 前缀）、填写正文、点击发布，最终检测到发布成功提示

#### Scenario: 发布页面 DOM 选择器失效
- **WHEN** Playwright 无法定位标题输入框、正文输入框或发布按钮
- **THEN** Spike 输出错误日志，包含当前页面截图保存路径，方便排查 DOM 变更

### Requirement: 可行性评估输出
Spike 运行结束后 SHALL 在控制台输出一份简要的可行性评估报告，包含每个步骤的通过/失败状态和 DOM 选择器稳定性评估。

#### Scenario: 所有步骤通过
- **WHEN** Spike 完整执行所有步骤且均成功
- **THEN** 控制台输出"可行性评估: PASS"，并列出每个步骤的耗时和使用的 DOM 选择器

#### Scenario: 部分步骤失败
- **WHEN** Spike 在某个步骤失败
- **THEN** 控制台输出"可行性评估: FAIL"，标注失败步骤和原因，已通过步骤仍输出选择器信息
