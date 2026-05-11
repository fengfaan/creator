import { useEffect, useRef, useState, useMemo, useCallback } from "react";
import { TopBar } from "./components/top-bar";
import { FileSidebar } from "./components/file-sidebar";
import { EditorPanel } from "./components/editor-panel";
import { PreviewPanel } from "./components/preview-panel";
import { PublishModal } from "./components/publish-modal";
import { DistributionConsole } from "./components/distribution-console";
import { SettingsModal } from "./components/settings-modal";
import { api } from "./api";

type MobilePane = "files" | "editor" | "preview";
type PlatformId = "wechat" | "xhs";

interface PlatformDrafts {
  wechat: string;
  xhs: string;
}

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [isDark, setIsDark] = useState(false);
  const [fileName, setFileName] = useState("");
  const [filePath, setFilePath] = useState("");
  const [activeId, setActiveId] = useState("");
  const [title, setTitle] = useState("");
  const [outline, setOutline] = useState("");
  const [drafts, setDrafts] = useState<PlatformDrafts>({ wechat: "", xhs: "" });
  const [activePlatform, setActivePlatform] = useState<PlatformId>("wechat");
  const [leftPct, setLeftPct] = useState(50);
  const [publishOpen, setPublishOpen] = useState(false);
  const [distOpen, setDistOpen] = useState(false);
  const [coverPath, setCoverPath] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [savedContent, setSavedContent] = useState("");
  const [mobilePane, setMobilePane] = useState<MobilePane>("editor");
  const [aiModel, setAiModel] = useState("deepseek-chat");
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
    setMobilePane("editor");
    try {
      const text = await api.files.readContent(path);
      const parsed = parseDraftFile(text);
      setOutline(parsed.outline);
      setDrafts(parsed.drafts);
      setActivePlatform(parsed.drafts.wechat.trim() || !parsed.drafts.xhs.trim() ? "wechat" : "xhs");
      setSavedContent(serializeDraftFile(parsed.outline, parsed.drafts));
      const heading = (parsed.drafts.wechat || parsed.drafts.xhs).match(/^#\s+(.+)/m);
      setTitle(heading ? heading[1] : name.replace(/\.md$/, ""));
    } catch {
      setOutline("");
      setDrafts({ wechat: "", xhs: "" });
      setTitle(name.replace(/\.md$/, ""));
    }
  }, []);

  const [saveStatus, setSaveStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");

  const doSave = useCallback(async (nextOutline: string, nextDrafts: PlatformDrafts) => {
    if (!filePath) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    setSaveStatus("saving");
    try {
      const serialized = serializeDraftFile(nextOutline, nextDrafts);
      await api.files.save(filePath, serialized);
      setSavedContent(serialized);
      setSaveStatus("saved");
      setTimeout(() => setSaveStatus("idle"), 2000);
    } catch (e) {
      console.error("Save failed:", e);
      setSaveStatus("error");
    }
  }, [filePath]);

  const queueSave = useCallback((nextOutline: string, nextDrafts: PlatformDrafts) => {
    if (!filePath) return;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(() => doSave(nextOutline, nextDrafts), 1000);
  }, [filePath, doSave]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "s") {
        e.preventDefault();
        doSave(outline, drafts);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [outline, drafts, doSave]);

  const handleContentChange = useCallback((newContent: string) => {
    setDrafts((current) => {
      const next = { ...current, [activePlatform]: newContent };
      queueSave(outline, next);
      return next;
    });
  }, [activePlatform, outline, queueSave]);

  const handlePlatformContentChange = useCallback((platform: PlatformId, newContent: string) => {
    setDrafts((current) => {
      const next = { ...current, [platform]: newContent };
      queueSave(outline, next);
      return next;
    });
  }, [outline, queueSave]);

  const handleTitleChange = useCallback((newTitle: string) => {
    setTitle(newTitle);
    setDrafts((current) => {
      const next = { ...current };
      for (const key of Object.keys(next) as PlatformId[]) {
        const draft = next[key];
        if (draft.startsWith("# ")) {
          const afterHeading = draft.indexOf("\n");
          next[key] = "# " + newTitle + (afterHeading >= 0 ? draft.slice(afterHeading) : "");
        } else {
          next[key] = "# " + newTitle + (draft ? "\n" + draft : "");
        }
      }
      queueSave(outline, next);
      return next;
    });
  }, [outline, queueSave]);

  const handleOutlineChange = useCallback((newOutline: string) => {
    setOutline(newOutline);
    queueSave(newOutline, drafts);
  }, [drafts, queueSave]);

  const activeContent = drafts[activePlatform];
  const wordCount = useMemo(() => activeContent.replace(/\s/g, "").length, [activeContent]);
  const lineCount = useMemo(() => activeContent.split("\n").length, [activeContent]);
  const isDirty = serializeDraftFile(outline, drafts) !== savedContent;

  return (
    <div className={isDark ? "dark" : ""}>
      <a href="#main-editor" className="skip-link">跳转到编辑器</a>
      <div
        className="flex flex-col h-screen w-full overflow-hidden"
        style={{ background: "var(--bg-deepest)", color: "var(--text-primary)" }}
      >
        <TopBar
          fileName={fileName}
          model={aiModel}
          isDirty={isDirty}
          saveStatus={saveStatus}
          onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          isDark={isDark}
          onToggleTheme={() => setIsDark(!isDark)}
          onSave={() => doSave(outline, drafts)}
          onPublish={() => {
            setDistOpen(true);
          }}
          onOpenSettings={() => setSettingsOpen(true)}
          onModelChange={setAiModel}
        />

        <div className="mobile-pane-switch" role="tablist" aria-label="移动端视图切换">
          {[
            ["files", "文件"],
            ["editor", "编辑"],
            ["preview", "预览"],
          ].map(([pane, label]) => (
            <button
              key={pane}
              type="button"
              role="tab"
              aria-selected={mobilePane === pane}
              className={mobilePane === pane ? "active" : ""}
              onClick={() => setMobilePane(pane as MobilePane)}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="workspace-shell flex flex-1 min-h-0">
          <div
            className={`sidebar-wrap${sidebarOpen ? "" : " collapsed"}`}
            data-mobile-active={mobilePane === "files"}
            style={{ width: 240 }}
          >
            <FileSidebar
              activeId={activeId}
              activePath={filePath}
              onSelect={loadFile}
            />
          </div>

          <div
            id="main-editor"
            ref={containerRef}
            className="main-workspace flex-1 flex min-w-0 min-h-0"
            data-mobile-pane={mobilePane}
            tabIndex={-1}
          >
            <div style={{ width: `${leftPct}%` }} className="editor-pane flex flex-col min-w-0 min-h-0">
              <EditorPanel
                title={title}
                setTitle={handleTitleChange}
                outline={outline}
                setOutline={handleOutlineChange}
                content={activeContent}
                setContent={handleContentChange}
                activePlatform={activePlatform}
                setActivePlatform={setActivePlatform}
                platformDrafts={drafts}
                setPlatformContent={handlePlatformContentChange}
                onCoverGenerated={setCoverPath}
              />
            </div>
            <div className="resize-divider" onMouseDown={startDrag} onDoubleClick={() => setLeftPct(50)} />
            <div style={{ width: `${100 - leftPct}%` }} className="preview-pane flex flex-col min-w-0 min-h-0">
              <PreviewPanel
                title={title}
                content={activeContent}
                activePlatform={activePlatform}
                outline={outline}
                platformDrafts={drafts}
              />
            </div>
          </div>
        </div>

        {distOpen && <DistributionConsole title={title} content={activeContent} coverPath={coverPath} onCoverPathChange={setCoverPath} />}

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
                style={{ background: saveStatus === "error" ? "var(--status-error)" : isDirty ? "var(--status-warning)" : "var(--status-success)" }}
                aria-hidden="true"
              />
              {saveStatus === "saving" ? "保存中..." : saveStatus === "saved" ? "已保存" : saveStatus === "error" ? "保存失败" : isDirty ? "未保存" : "已自动保存"}
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

        <PublishModal open={publishOpen} onClose={() => setPublishOpen(false)} title={title} content={activeContent} />
        <SettingsModal open={settingsOpen} onClose={() => setSettingsOpen(false)} onSaved={setAiModel} />
      </div>
    </div>
  );
}

const OUTLINE_START = "<!-- AI_WRITER_OUTLINE";
const OUTLINE_END = "AI_WRITER_OUTLINE -->";
const PLATFORM_START = "<!-- AI_WRITER_PLATFORM:";
const PLATFORM_END = "AI_WRITER_PLATFORM -->";

function parseDraftFile(text: string) {
  const trimmed = text || "";
  const drafts = parsePlatformDrafts(trimmed);
  if (!trimmed.startsWith(OUTLINE_START)) {
    return { outline: "", drafts: drafts.found ? drafts.drafts : { wechat: trimmed, xhs: "" } };
  }
  const end = trimmed.indexOf(OUTLINE_END);
  if (end < 0) {
    return { outline: "", drafts: drafts.found ? drafts.drafts : { wechat: trimmed, xhs: "" } };
  }
  const outline = trimmed.slice(OUTLINE_START.length, end).replace(/^\s*\n/, "").replace(/\n\s*$/, "");
  const body = trimmed.slice(end + OUTLINE_END.length).replace(/^\s*\n/, "");
  const parsedDrafts = parsePlatformDrafts(body);
  return {
    outline,
    drafts: parsedDrafts.found ? parsedDrafts.drafts : { wechat: body, xhs: "" },
  };
}

function parsePlatformDrafts(text: string): { found: boolean; drafts: PlatformDrafts } {
  const drafts: PlatformDrafts = { wechat: "", xhs: "" };
  const pattern = new RegExp(`${escapeRegExp(PLATFORM_START)}(wechat|xhs)\\s*-->\\n?([\\s\\S]*?)\\n?<!--\\s*${escapeRegExp(PLATFORM_END)}`, "g");
  let found = false;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text))) {
    found = true;
    drafts[match[1] as PlatformId] = match[2].trim();
  }
  return { found, drafts };
}

function serializeDraftFile(outline: string, drafts: PlatformDrafts) {
  const safeOutline = outline.trim();
  const wechat = drafts.wechat || "";
  const xhs = drafts.xhs || "";
  const body = `${PLATFORM_START}wechat -->\n${wechat.trim()}\n<!-- ${PLATFORM_END}\n\n${PLATFORM_START}xhs -->\n${xhs.trim()}\n<!-- ${PLATFORM_END}`;
  if (!safeOutline) {
    return body;
  }
  return `${OUTLINE_START}\n${safeOutline}\n${OUTLINE_END}\n\n${body}`;
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
