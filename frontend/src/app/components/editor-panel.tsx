import { useMemo, useRef, useState, useEffect } from "react";
import { Sparkles, FileText, Expand, Wand2, ArrowRight, Languages, Image as ImageIcon, Table, Code, Brain, PenLine } from "lucide-react";
import { AIPanel } from "./ai-panel";

interface Props {
  title: string;
  setTitle: (v: string) => void;
  content: string;
  setContent: (v: string) => void;
}

interface SlashItem {
  cmd: string;
  desc: string;
  shortcut?: string;
  type: "ai" | "insert";
  icon: React.ReactNode;
  insertText?: string;
}

const SLASH_ITEMS: SlashItem[] = [
  { cmd: "/大纲", desc: "生成文章大纲", shortcut: "⌘1", type: "ai", icon: <FileText size={14} strokeWidth={1.5} /> },
  { cmd: "/扩写", desc: "扩展选定段落", shortcut: "⌘2", type: "ai", icon: <Expand size={14} strokeWidth={1.5} /> },
  { cmd: "/润色", desc: "小红书风格改写", shortcut: "⌘3", type: "ai", icon: <Wand2 size={14} strokeWidth={1.5} /> },
  { cmd: "/续写", desc: "AI 续写下文", shortcut: "⌘4", type: "ai", icon: <ArrowRight size={14} strokeWidth={1.5} /> },
  { cmd: "/翻译", desc: "翻译为英文", shortcut: "⌘5", type: "ai", icon: <Languages size={14} strokeWidth={1.5} /> },
  { cmd: "/图片", desc: "插入本地图片", type: "insert", icon: <ImageIcon size={14} strokeWidth={1.5} />, insertText: "![](image.png)" },
  { cmd: "/表格", desc: "插入表格", type: "insert", icon: <Table size={14} strokeWidth={1.5} />, insertText: "| 列1 | 列2 |\n| --- | --- |\n| 内容 | 内容 |" },
  { cmd: "/代码块", desc: "插入代码", type: "insert", icon: <Code size={14} strokeWidth={1.5} />, insertText: "```\n\n```" },
];

export function EditorPanel({ title, setTitle, content, setContent }: Props) {
  const [showAI, setShowAI] = useState(false);
  const [slash, setSlash] = useState<{ open: boolean; query: string; pos: { x: number; y: number }; triggerStart: number }>({
    open: false,
    query: "",
    pos: { x: 0, y: 0 },
    triggerStart: -1,
  });
  const [activeIdx, setActiveIdx] = useState(0);
  const taRef = useRef<HTMLTextAreaElement>(null);
  const wrapRef = useRef<HTMLDivElement>(null);

  const filtered = useMemo(() => {
    if (!slash.query) return SLASH_ITEMS;
    const q = slash.query.toLowerCase();
    return SLASH_ITEMS.filter(
      (it) => it.cmd.toLowerCase().includes(q) || it.desc.toLowerCase().includes(q)
    );
  }, [slash.query]);

  const groups = useMemo(() => {
    const ai = filtered.filter((it) => it.type === "ai");
    const ins = filtered.filter((it) => it.type === "insert");
    return { ai, ins };
  }, [filtered]);

  const insertAtCursor = (text: string) => {
    const ta = taRef.current;
    if (!ta) {
      setContent(content + text);
      return;
    }
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const next = content.slice(0, start) + text + content.slice(end);
    setContent(next);
    setTimeout(() => {
      ta.focus();
      const pos = start + text.length;
      ta.setSelectionRange(pos, pos);
    }, 0);
  };

  const replaceRange = (from: number, to: number, text: string) => {
    const next = content.slice(0, from) + text + content.slice(to);
    setContent(next);
    setTimeout(() => {
      const ta = taRef.current;
      if (ta) {
        ta.focus();
        const pos = from + text.length;
        ta.setSelectionRange(pos, pos);
      }
    }, 0);
  };

  const lines = useMemo(() => content.split("\n"), [content]);

  const highlighted = useMemo(() => {
    return lines.map((line, i) => {
      let className = "";
      if (/^#{1,6}\s/.test(line)) className = "md-heading";
      else if (/^>\s/.test(line)) className = "md-quote";
      else if (/^(\s*)([-*+]|\d+\.)\s/.test(line)) className = "md-list";
      else if (/^```/.test(line)) className = "md-code-fence";

      return (
        <div key={i} className="flex">
          <span
            style={{
              width: 32,
              textAlign: "right",
              paddingRight: 12,
              color: "var(--text-muted)",
              userSelect: "none",
              fontSize: 13,
            }}
          >
            {i + 1}
          </span>
          <span className={`md-line ${className}`} style={{ whiteSpace: "pre-wrap", flex: 1 }}>
            {renderInline(line) || "​"}
          </span>
        </div>
      );
    });
  }, [lines]);

  const updateSlashFromCaret = (ta: HTMLTextAreaElement) => {
    const cursor = ta.selectionStart;
    const before = content.slice(0, cursor);
    const m = before.match(/(?:^|[\s])\/([^\s/]*)$/);
    if (!m) {
      if (slash.open) setSlash((s) => ({ ...s, open: false }));
      return;
    }
    const slashIdx = before.length - m[0].length + (m[0].startsWith("/") ? 0 : 1);
    const query = m[1];

    const coords = getCaretCoordinates(ta, slashIdx);
    const wrap = wrapRef.current?.getBoundingClientRect();
    const taRect = ta.getBoundingClientRect();
    const x = (taRect.left - (wrap?.left || 0)) + coords.left;
    const y = (taRect.top - (wrap?.top || 0)) + coords.top + coords.height + 4;

    setSlash({ open: true, query, pos: { x, y }, triggerStart: slashIdx });
    setActiveIdx(0);
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    requestAnimationFrame(() => {
      if (taRef.current) updateSlashFromCaret(taRef.current);
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (slash.open && filtered.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIdx((i) => (i + 1) % filtered.length);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIdx((i) => (i - 1 + filtered.length) % filtered.length);
      } else if (e.key === "Enter") {
        e.preventDefault();
        runSlashItem(filtered[activeIdx]);
      } else if (e.key === "Escape") {
        e.preventDefault();
        setSlash((s) => ({ ...s, open: false }));
      }
    }
  };

  const runSlashItem = (it: SlashItem) => {
    const ta = taRef.current;
    if (!ta) return;
    const cursor = ta.selectionStart;
    const from = slash.triggerStart;
    const to = cursor;
    setSlash((s) => ({ ...s, open: false }));

    if (it.type === "ai") {
      replaceRange(from, to, "");
      setShowAI(true);
    } else {
      replaceRange(from, to, it.insertText || "");
    }
  };

  return (
    <div ref={wrapRef} className="flex-1 flex flex-col min-w-0 min-h-0 relative" style={{ background: "var(--bg-surface)" }}>
      <div className="flex-1 overflow-y-auto min-h-0">
        <div className="mx-auto" style={{ maxWidth: 760, padding: "32px 32px 120px" }}>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="输入标题..."
            aria-label="文章标题"
            className="w-full bg-transparent outline-none border-0 focus-visible:ring-0"
            style={{
              fontSize: 28,
              fontWeight: 700,
              lineHeight: 1.3,
              color: "var(--text-primary)",
            }}
          />
          <div className="my-4" style={{ borderTop: "1px solid var(--border-subtle)" }} />

          <div className="relative" style={{ fontFamily: "var(--font-mono)", fontSize: 14, lineHeight: 1.7 }}>
            <div
              aria-hidden
              className="absolute inset-0 pointer-events-none"
              style={{ color: "var(--text-primary)" }}
            >
              {highlighted}
            </div>
            <textarea
              ref={taRef}
              value={content}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              onClick={(e) => updateSlashFromCaret(e.currentTarget)}
              onKeyUp={(e) => {
                if (!["ArrowDown", "ArrowUp", "Enter"].includes(e.key)) {
                  updateSlashFromCaret(e.currentTarget);
                }
              }}
              spellCheck={false}
              className="relative w-full bg-transparent outline-none resize-none"
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 14,
                lineHeight: 1.7,
                color: "transparent",
                caretColor: "var(--accent-primary)",
                paddingLeft: 32,
                minHeight: 600,
              }}
              rows={Math.max(20, lines.length + 2)}
            />
          </div>
        </div>
      </div>

      {slash.open && filtered.length > 0 && (
        <div
          className="absolute z-30 rounded-lg overflow-hidden"
          style={{
            left: Math.max(16, slash.pos.x),
            top: slash.pos.y,
            width: 300,
            maxHeight: 360,
            overflowY: "auto",
            background: "var(--bg-elevated)",
            border: "1px solid var(--border-default)",
            boxShadow: "0 8px 32px rgba(0,0,0,0.12)",
          }}
        >
          {groups.ai.length > 0 && (
            <>
              <GroupHeader label="AI 操作" icon={<Brain size={11} strokeWidth={1.5} />} color="var(--accent-ai)" bg="var(--accent-ai-light)" />
              {groups.ai.map((it) => {
                const idx = filtered.indexOf(it);
                return <SlashRow key={it.cmd} item={it} active={idx === activeIdx} onClick={() => runSlashItem(it)} />;
              })}
            </>
          )}
          {groups.ai.length > 0 && groups.ins.length > 0 && (
            <div style={{ height: 1, background: "var(--border-subtle)" }} />
          )}
          {groups.ins.length > 0 && (
            <>
              <GroupHeader label="插入" icon={<PenLine size={11} strokeWidth={1.5} />} color="var(--text-muted)" bg="var(--bg-deepest)" />
              {groups.ins.map((it) => {
                const idx = filtered.indexOf(it);
                return <SlashRow key={it.cmd} item={it} active={idx === activeIdx} onClick={() => runSlashItem(it)} />;
              })}
            </>
          )}
        </div>
      )}

      {!showAI && (
        <button
          onClick={() => setShowAI(true)}
          className="absolute bottom-5 right-5 flex items-center gap-1.5 h-8 px-3 rounded-full transition-all hover:opacity-90 z-30"
          style={{
            background: "var(--accent-ai)",
            color: "#fff",
            fontSize: 12,
            fontWeight: 500,
            boxShadow: "0 2px 10px rgba(139,92,246,0.25)",
          }}
        >
          <Sparkles size={13} strokeWidth={1.5} />
          AI 助写
        </button>
      )}

      {showAI && (
        <AIPanel
          onInsert={(text) => {
            insertAtCursor(text);
            setShowAI(false);
          }}
          onClose={() => setShowAI(false)}
        />
      )}
    </div>
  );
}

function GroupHeader({ label, icon, color, bg }: { label: string; icon: React.ReactNode; color: string; bg: string }) {
  return (
    <div
      className="px-3 py-1.5 flex items-center gap-1"
      style={{
        fontSize: 11,
        fontWeight: 600,
        color,
        letterSpacing: 0.5,
        background: bg,
        position: "sticky",
        top: 0,
      }}
    >
      {icon} {label}
    </div>
  );
}

function SlashRow({ item, active, onClick }: { item: SlashItem; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex items-center justify-between w-full px-3 h-11 transition-colors text-left hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
      style={{
        background: active ? "var(--bg-hover)" : "transparent",
      }}
    >
      <div className="flex items-center gap-2 min-w-0">
        <span
          className="w-5 h-5 flex items-center justify-center shrink-0 rounded"
          style={{
            color: item.type === "ai" ? "var(--accent-ai)" : "var(--accent-primary)",
            background: item.type === "ai" ? "var(--accent-ai-light)" : "var(--accent-primary-light)",
          }}
        >
          {item.icon}
        </span>
        <span
          style={{
            fontFamily: "var(--font-mono)",
            fontSize: 12,
            color: item.type === "ai" ? "var(--accent-ai)" : "var(--accent-primary)",
          }}
        >
          {item.cmd}
        </span>
        <span style={{ fontSize: 13, color: "var(--text-primary)" }} className="truncate">
          {item.desc}
        </span>
      </div>
      {item.shortcut && (
        <span style={{ fontSize: 11, color: "var(--text-muted)" }} className="shrink-0 ml-2">
          {item.shortcut}
        </span>
      )}
    </button>
  );
}

function renderInline(line: string) {
  const parts: (string | JSX.Element)[] = [];
  const regex = /(\*\*[^*]+\*\*)|(\*[^*]+\*)|(`[^`]+`)|(\[[^\]]+\]\([^)]+\))|(#\w+)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let key = 0;
  while ((m = regex.exec(line))) {
    if (m.index > last) parts.push(line.slice(last, m.index));
    const tok = m[0];
    if (tok.startsWith("**")) parts.push(<span key={key++} className="md-bold">{tok}</span>);
    else if (tok.startsWith("*")) parts.push(<span key={key++} className="md-italic">{tok}</span>);
    else if (tok.startsWith("`")) parts.push(<span key={key++} className="md-inline-code">{tok}</span>);
    else if (tok.startsWith("[")) parts.push(<span key={key++} className="md-link">{tok}</span>);
    else if (tok.startsWith("#")) parts.push(<span key={key++} className="md-tag">{tok}</span>);
    last = m.index + tok.length;
  }
  if (last < line.length) parts.push(line.slice(last));
  return <>{parts}</>;
}

function getCaretCoordinates(el: HTMLTextAreaElement, position: number) {
  const div = document.createElement("div");
  const style = window.getComputedStyle(el);
  const props = [
    "boxSizing", "width", "height", "overflowX", "overflowY",
    "borderTopWidth", "borderRightWidth", "borderBottomWidth", "borderLeftWidth",
    "paddingTop", "paddingRight", "paddingBottom", "paddingLeft",
    "fontStyle", "fontVariant", "fontWeight", "fontStretch", "fontSize", "fontSizeAdjust",
    "lineHeight", "fontFamily", "textAlign", "textTransform", "textIndent", "textDecoration",
    "letterSpacing", "wordSpacing", "tabSize", "MozTabSize",
  ];
  div.style.position = "absolute";
  div.style.visibility = "hidden";
  div.style.whiteSpace = "pre-wrap";
  div.style.wordWrap = "break-word";
  props.forEach((p) => {
    (div.style as any)[p] = (style as any)[p];
  });
  div.textContent = el.value.substring(0, position);
  const span = document.createElement("span");
  span.textContent = el.value.substring(position) || ".";
  div.appendChild(span);
  document.body.appendChild(div);
  const rect = { top: span.offsetTop, left: span.offsetLeft, height: parseInt(style.lineHeight) || 20 };
  document.body.removeChild(div);
  return rect;
}
