## 1. 环境准备

- [ ] 1.1 在 `pom.xml` 中添加 Playwright for Java 依赖 (`com.microsoft.playwright:playwright:1.49.0`) 和 JUnit 5 测试依赖
- [ ] 1.2 准备测试图片资源 `src/test/resources/spike-cover.jpg`（一张用于上传的测试封面图）
- [ ] 1.3 运行 `mvn test-compile` 确认依赖下载成功，Playwright CLI 可用

## 2. Spike 脚本核心实现

- [ ] 2.1 创建 `XhsPublishSpike.java` 测试类骨架，包含 Playwright lifecycle 管理（setup/teardown）
- [ ] 2.2 实现 session 持久化逻辑：检测 `~/.aiwriter/rpa-session/xhs-state.json` 是否存在，存在则加载，不存在则启动 headed 模式等待扫码
- [ ] 2.3 实现登录态检测：打开 `creator.xiaohongshu.com/publish/publish`，检测是否被重定向到登录页
- [ ] 2.4 实现图片上传步骤：定位文件上传 input，使用 `setInputFiles()` 上传测试封面图
- [ ] 2.5 实现填写标题步骤：定位标题输入框，填入带 `[TEST]` 前缀的测试标题
- [ ] 2.6 实现填写正文步骤：定位正文输入框，填入测试正文内容
- [ ] 2.7 实现点击发布步骤：定位发布按钮并点击，等待发布成功提示或页面跳转

## 3. 可行性评估

- [ ] 3.1 在每个步骤添加选择器信息日志（使用的选择器 + 耗时 + 通过/失败）
- [ ] 3.2 失败时自动截图保存到 `target/rpa-spike-screenshots/`
- [ ] 3.3 运行完整 Spike，记录可行性评估结果（控制台输出汇总）
