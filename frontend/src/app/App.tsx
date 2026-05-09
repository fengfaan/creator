import { useEffect, useRef, useState, useMemo, useCallback } from "react";
import { TopBar } from "./components/top-bar";
import { FileSidebar } from "./components/file-sidebar";
import { EditorPanel } from "./components/editor-panel";
import { PreviewPanel } from "./components/preview-panel";
import { PublishModal } from "./components/publish-modal";
import { api } from "./api";

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isDark, setIsDark] = useState(false);
  const [fileName, setFileName] = useState("");
  const [filePath, setFilePath] = useState("");
  const [activeId, setActiveId] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [leftPct, setLeftPct] = useState(50);
  const [publishOpen, setPublishOpen] = useState(false);
  const [savedContent, setSavedContent] = useState("");
  const dragRef = useRef<{ dragging: boolean; startX: number; startPct: number }>({
    dragging: false,
    startX: 0,
    startPct: 50,
  });
  const containerRef = useRef<HTMLDivElement>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout>>();

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

  const loadFile = useCallback(async (id: string, name: string, path: string) => {
    setActiveId(id);
    setFileName(name);
    setFilePath(path);
    try {
      const text = await api.files.readContent(path);
      setContent(text);
      setSavedContent(text);
      const heading = text.match(/^#\s+(.+)/m);
      setTitle(heading ? heading[1] : name.replace(/\.md$/, ""));
    } catch {
      setContent("");
      setTitle(name.replace(/\.md$/, ""));
    }
  }, []);

  const handleContentChange = useCallback((newContent: string) => {
    setContent(newContent);
    if (!filePath) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(async () => {
      try {
        await api.files.save(filePath, newContent);
        setSavedContent(newContent);
      } catch (e) {
        console.error("Auto-save failed:", e);
      }
    }, 1000);
  }, [filePath]);

  const wordCount = useMemo(() => content.replace(/\s/g, "").length, [content]);
  const lineCount = useMemo(() => content.split("\n").length, [content]);
  const isDirty = content !== savedContent;

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
              activePath={filePath}
              onSelect={loadFile}
            />
          </div>

          <div id="main-editor" ref={containerRef} className="flex-1 flex min-w-0 min-h-0" tabIndex={-1}>
            <div style={{ width: `${leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <EditorPanel title={title} setTitle={setTitle} content={content} setContent={handleContentChange} />
            </div>
            <div className="resize-divider" onMouseDown={startDrag} onDoubleClick={() => setLeftPct(50)} />
            <div style={{ width: `${100 - leftPct}%` }} className="flex flex-col min-w-0 min-h-0">
              <PreviewPanel title={title} content={content} />
            </div>
          </div>
        </div>

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
                style={{ background: isDirty ? "var(--status-warning)" : "var(--status-success)" }}
                aria-hidden="true"
              />
              {isDirty ? "未保存" : "已自动保存"}
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
