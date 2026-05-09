import { useEffect, useRef, useState, useMemo } from "react";
import { TopBar } from "./components/top-bar";
import { FileSidebar } from "./components/file-sidebar";
import { EditorPanel } from "./components/editor-panel";
import { PreviewPanel } from "./components/preview-panel";
import { PublishModal } from "./components/publish-modal";

const SAMPLE = `# 如何写好一篇小红书爆款笔记

> 内容创作不是玄学，是可拆解的方法论。

## 一、选题决定 80% 的成败

爆款笔记往往源自一个**好选题**。一个吸引人的选题应当具备：

- 强烈的情绪共鸣
- 明确的目标人群
- 可视化的内容形态
- 与平台调性相符

## 二、标题公式

\`\`\`
痛点 + 数字 + 解决方案 = 高点击率
\`\`\`

例如："**3 步搞定**周报，效率提升 200%"。

## 三、配图与排版

封面图至关重要，建议使用 [Figma](https://figma.com) 设计。

> 记住：好内容值得被看见，认真打磨每一处细节。

#小红书 #内容创作 #自媒体
`;

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isDark, setIsDark] = useState(false);
  const [fileName, setFileName] = useState("如何写好小红书.md");
  const [activeId, setActiveId] = useState("f1");
  const [title, setTitle] = useState("如何写好一篇小红书爆款笔记");
  const [content, setContent] = useState(SAMPLE);
  const [leftPct, setLeftPct] = useState(50);
  const [publishOpen, setPublishOpen] = useState(false);
  const dragRef = useRef<{ dragging: boolean; startX: number; startPct: number }>({
    dragging: false,
    startX: 0,
    startPct: 50,
  });
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!dragRef.current.dragging || !containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const dx = e.clientX - dragRef.current.startX;
      const newPct = dragRef.current.startPct + (dx / rect.width) * 100;
      setLeftPct(Math.max(30, Math.min(75, newPct)));
    };
    const onUp = () => {
      dragRef.current.dragging = false;
      document.body.style.cursor = "";
      document.querySelectorAll(".resize-divider").forEach((el) => el.classList.remove("dragging"));
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
    return () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
    };
  }, []);

  const startDrag = (e: React.MouseEvent) => {
    dragRef.current = { dragging: true, startX: e.clientX, startPct: leftPct };
    document.body.style.cursor = "col-resize";
    (e.currentTarget as HTMLElement).classList.add("dragging");
  };

  const wordCount = useMemo(() => content.replace(/\s/g, "").length, [content]);
  const lineCount = useMemo(() => content.split("\n").length, [content]);

  return (
    <div className={isDark ? "dark" : ""}>
      <a href="#main-editor" className="skip-link">跳转到编辑器</a>
      <div
        className="flex flex-col h-screen w-full overflow-hidden"
        style={{ background: "var(--bg-deepest)", color: "var(--text-primary)" }}
      >
        <TopBar
          fileName={fileName}
          onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          isDark={isDark}
          onToggleTheme={() => setIsDark(!isDark)}
          onPublish={() => setPublishOpen(true)}
        />

        <div className="flex flex-1 min-h-0">
          <div className={`sidebar-wrap${sidebarOpen ? "" : " collapsed"}`} style={{ width: 240 }}>
            <FileSidebar
              activeId={activeId}
              onSelect={(id, name) => {
                setActiveId(id);
                setFileName(name);
              }}
            />
          </div>

          <div id="main-editor" ref={containerRef} className="flex-1 flex min-w-0 min-h-0" tabIndex={-1}>
            <div style={{ width: `${leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <EditorPanel title={title} setTitle={setTitle} content={content} setContent={setContent} />
            </div>
            <div className="resize-divider" onMouseDown={startDrag} onDoubleClick={() => setLeftPct(50)} />
            <div style={{ width: `${100 - leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <PreviewPanel title={title} content={content} />
            </div>
          </div>
        </div>

        {/* Status bar */}
        <div
          className="flex items-center justify-between h-7 px-4 shrink-0"
          role="status"
          style={{
            background: "var(--bg-deepest)",
            borderTop: "1px solid var(--border-subtle)",
            fontSize: 12,
            color: "var(--text-secondary)",
            fontFamily: "var(--font-mono)",
          }}
        >
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <span
                className="inline-block w-1.5 h-1.5 rounded-full"
                style={{ background: "var(--status-success)" }}
                aria-hidden="true"
              />
              已自动保存
            </span>
            <span>UTF-8</span>
            <span>Markdown</span>
          </div>
          <div className="flex items-center gap-3">
            <span>{wordCount} 字</span>
            <span>{lineCount} 行</span>
            <span>Ln {lineCount}, Col 1</span>
          </div>
        </div>

        <PublishModal open={publishOpen} onClose={() => setPublishOpen(false)} title={title} />
      </div>
    </div>
  );
}
