## Why

MVP-1 已完成真实 AI 助写，但发布弹窗仍是模拟自动发布。MVP-2 需要把发布入口收敛成可用的发布前准备流程：先检查平台格式，再导出对应内容，最后打开平台后台由用户手动完成发布。

## What Changes

- 发布弹窗新增平台格式检查：标题长度、正文长度、违禁词占位规则
- 支持导出公众号 HTML 和小红书正文
- 发布流程改为半自动：复制内容并打开平台后台
- 移除弹窗内“直接发布成功”的模拟流程

## Impact

- 修改 `frontend/src/app/components/publish-modal.tsx`
- 修改 `frontend/src/app/App.tsx`，将正文传给发布弹窗
- 不新增后端 API
