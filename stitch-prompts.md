# Stitch 设计提示词 - 个人智能写作与分发工作台

> 以下每个 Prompt 对应一个独立页面/状态，可逐条粘贴到 Stitch 中生成。
> 每条 Prompt 都是自包含的，包含完整的色彩、字体和布局描述。

---

## 全局设计规范 (每条 Prompt 开头复用)

```
Design system: Modern warm white style like Notion. Background #FAFAFA, cards #FFFFFF, borders #E5E5E5. Primary color teal #0D9488, CTA orange #EA580C, AI purple #8B5CF6, WeChat green #07C160, Xiaohongshu red #FF2442. Text #1A1A1A primary, #6B7280 secondary. Font: Plus Jakarta Sans for UI, JetBrains Mono for code/editor. Border radius 8px for cards/buttons, 6px for inputs, 12px for modals. No heavy shadows, flat solid color blocks. Use Lucide line icons only, no emojis.
```

---

## Prompt 1: PC 主工作台 — 默认态

```
Design a desktop web app main workspace (1440x900) for a Markdown writing and publishing tool.

[Global top bar] 48px height, white background, bottom border #E5E5E5. Left: collapse sidebar icon (24px), text logo "AI Writer" (Plus Jakarta Sans 16px semi-bold). Center: green dot indicator + text "DeepSeek-V3 online" (purple dot #8B5CF6). Right: model dropdown (180x32px, rounded 4px), sun/moon theme toggle icon, settings gear icon.

[Left sidebar] 240px wide, white background, right border #E5E5E5. Top: search box (36px, placeholder "Search files..."). Below: "+ New Document" text button in teal #0D9488. Below: file tree with folders and .md files. Selected file has teal #0D9488 left border 3px + light teal background #F0FDFA. Hover shows #F5F5F5 background. Bottom section shows "Published" group with platform-colored tags (green WeChat, red Xiaohongshu).

[Center editor panel] White background, content max-width 720px centered with 32px horizontal padding. Top: large title input "Enter title..." in Plus Jakarta Sans 28px bold, no border. Below: thin divider #F0F0F0. Below: Markdown editor area in JetBrains Mono 14px, line-height 1.7, with line numbers on left (24px wide, #9CA3AF). Syntax highlighting: headings blue #569CD6, bold orange #CE9178, links cyan #4EC9B0, code gray, quotes green, lists yellow. Cursor: teal #0D9488 blinking line.

[Right panel] White background, top has tab bar (44px): "WeChat Preview" (default selected, green #07C160 bottom underline) | "Xiaohongshu Preview" (red #FF2442 underline) | "AI Check" button on right (#8B5CF6). Below tabs: preview area showing WeChat article style rendering (max-width 375px centered, white background, title 22px bold, body 17px, images auto-width).

[Bottom of right panel] Fixed distribution console. Collapsed state: 44px header "▶ Distribution Console" with expand arrow. Expanded state: two buttons (green "Publish to WeChat", red "Publish to Xiaohongshu"), and dark terminal log area (#1A1A1A background) with monospace text showing timestamps and status messages in green/red/yellow.

[Resizable divider] 1px #E5E5E5 line between center and right panels, shows teal accent on hover.
```

---

## Prompt 2: PC 主工作台 — Slash Command 展开态

```
Design the same desktop writing workspace, but now the user has typed "/" in the editor.

[Slash command menu] A floating popup appears below the cursor position. Width 280px, white background #FFFFFF, rounded 8px, shadow 0 8px 32px rgba(0,0,0,0.1).

Two groups:
Group 1 "AI Operations" (purple #8B5CF6 label):
- /outline — Generate article outline
- /expand — Expand selected paragraph
- /polish — Xiaohongshu style rewrite
- /continue — AI continue writing
- /translate — Translate to English
Each item: 48px height, 20px icon on left, 14px text, hover shows #F5F5F5 background.

Group 2 "Insert" (gray label):
- /image — Insert local image
- /table — Insert table
- /codeblock — Insert code block

Divider between groups: 1px #E5E5E5.
Support keyboard up/down selection, Enter to confirm, Esc to close.
```

---

## Prompt 3: AI 浮动工具栏

```
Design a floating toolbar that appears when text is selected in the Markdown editor.

[Toolbar] Appears above the selected text, centered horizontally. Width auto-fit, height 36px, white background, rounded 8px, shadow 0 4px 16px rgba(0,0,0,0.08).

Icons from left to right: Bold (B), Italic (I), Strikethrough (S), Quote ("), Code (</>), divider, AI button.

AI button has a purple #8B5CF6 left border divider. Clicking it expands a submenu with AI operations: Rewrite, Expand, Polish, Translate.

All icons are 20px Lucide line icons. Hover: #F5F5F5 background, 150ms transition.
```

---

## Prompt 4: 小红书预览模式

```
Design the same workspace but with the "Xiaohongshu Preview" tab selected in the right panel.

[Right panel — Xiaohongshu preview] The tab "Xiaohongshu Preview" is now active with red #FF2442 bottom underline. The WeChat tab is inactive (gray text).

Preview area shows a Xiaohongshu-style card:
- Max-width 375px centered
- Rounded 12px card with white background
- Top: cover image area in 3:4 aspect ratio (gray placeholder with upload icon)
- Below: bold title 16px, body text 14px with short paragraphs and emojis
- Tags like #lifestyle #food highlighted in red #FF2442
- Bottom: interaction stats placeholder (heart 0, comment 0, bookmark 0)

The card sits inside a light gray #FAFAFA container with 24px padding.
```

---

## Prompt 5: 发布执行中状态

```
Design the workspace in "publishing in progress" state.

[Distribution console] Expanded to 240px height. The green "Publish to WeChat" button is in loading state: disabled with reduced saturation, spinner icon on left, text changed to "Publishing...".

[Terminal log] Dark area #1A1A1A showing monospace log lines:
> [12:01:03] OK Connecting to Chrome... (green #059669)
> [12:01:05] OK Uploading images (1/3)... (green)
> [12:01:08] OK Filling content... (green)
> [12:01:12] !! Error: image upload failed (red #DC2626)
> [12:01:15] .. Retrying... (yellow #D97706)
> [12:01:18] OK Published successfully! (green)

Font: JetBrains Mono 12px, text color #D1D5DB, timestamps #6B7280.
Auto-scroll to bottom. Top has 16px fade gradient from transparent to #1A1A1A.
```

---

## Prompt 6: 移动端编辑页 (375x812)

```
Design a mobile web app editing page (375x812) for a Markdown writing tool.

[Top bar] 48px, white background. Left: back arrow icon (24px) + filename "My Article.md" (14px, truncated). Right: "Saved" text in gray #6B7280, 13px.

[Editor] Full-screen Markdown editor. 16px font (system monospace), line-height 1.7, 20px horizontal padding, 16px top padding. No line numbers. Title input at top: 24px bold, no border, placeholder "Enter title...".

[Bottom toolbar] 52px, white background, top border #E5E5E5, safe area padding for iPhone. Five equally spaced icons (24px Lucide): Format (Type), List, Code, Image, Undo.

[Floating action button] 56px circle, centered at bottom, raised 28px above toolbar. Background: teal #0D9488, icon: Sparkles 24px white, shadow: 0 4px 16px rgba(13,148,136,0.3).
```

---

## Prompt 7: 移动端 Bottom Sheet 展开

```
Design a mobile bottom sheet (375x812) that opens after tapping the floating action button.

[Background overlay] 50% black rgba(0,0,0,0.5) covering full screen.

[Bottom sheet] White background, top rounded corners 16px, max height 70% of screen.

Top: drag handle bar (32x4px gray #D1D5DB, centered, 40px padding).

Content sections:
Section "AI Operations" (group label):
- Generate outline (48px row, 16px icon + 16px text)
- Expand paragraph
- Xiaohongshu polish
- AI continue writing
- Translate to English

Divider: 1px #E5E5E5

Section "Multi-platform Preview":
- Tap to navigate to preview page

Divider

Section "Execute Publishing":
- Publish to WeChat (green #07C160 icon)
- Publish to Xiaohongshu (red #FF2442 icon)

Bottom: safe area padding. Swipe down to dismiss.
```

---

## Prompt 8: 移动端预览页 (375x812)

```
Design a mobile preview page (375x812) for a writing tool.

[Top bar] 48px, white background. Left: back arrow + "Preview" title. 

[Segmented control] 32px height, rounded 8px, #F5F5F5 background. Two segments: "WeChat" (active, white pill with shadow) and "Xiaohongshu" (inactive, gray text).

[Preview area] Full-width, 16px padding. Shows either:
- WeChat style: white background, title 22px bold, body 17px, images full-width, 1.75 line-height
- Xiaohongshu card: rounded 12px, cover image 3:4 ratio, title 16px bold, body 14px, red tags, interaction stats at bottom

Both preview styles adapt to full mobile width (no 375px max constraint on mobile).
```

---

## Prompt 9: 设置页

```
Design a settings page for a writing tool, desktop layout (1440x900).

[Layout] Max-width 960px centered. Left navigation (200px) + right content area.

[Left nav] White background, 5 items with icons:
- AI Model Config (Brain icon) — active, teal #0D9488 left border + light teal bg
- Publishing Accounts (Users icon)
- Editor Preferences (Sliders icon)
- Data Management (Database icon)
- About (Info icon)

[Right content — AI Model Config] Heading: "AI Model Configuration" (Plus Jakarta Sans 22px bold).

Three cards stacked vertically (white background, 1px #E5E5E5 border, rounded 8px, 24px padding):

Card "DeepSeek":
- Label "API Key" → password input (40px, eye toggle to show/hide)
- Label "Model Select" → dropdown "deepseek-chat (default)"
- "Test Connection" button (outline style: transparent bg, teal border + text)

Card "Claude": same structure
Card "Qwen": same structure

Input focus state: teal #0D9488 border + 3px teal ring rgba(13,148,136,0.12).
```

---

## 使用说明

1. 将每条 Prompt 完整复制粘贴到 Stitch 中
2. 每条 Prompt 开头附上 "全局设计规范" 块，确保色彩/字体一致
3. 优先生成 Prompt 1 (主工作台默认态)，其他页面基于它微调
4. 如果 Stitch 对中文内容渲染不佳，可将中文文字替换为英文占位符
5. 生成后在 Figma 中手动微调间距和细节
