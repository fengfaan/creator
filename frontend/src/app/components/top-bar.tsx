import { PanelLeft, Settings, Sun, Moon, ChevronDown, Check, Send } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { api } from "../api";

interface TopBarProps {
  fileName: string;
  onToggleSidebar: () => void;
  isDark: boolean;
  onToggleTheme: () => void;
  onPublish: () => void;
}

const MODELS = ["DeepSeek-V3", "Claude Sonnet 4.6", "Qwen-Max"];

export function TopBar({ fileName, onToggleSidebar, isDark, onToggleTheme, onPublish }: TopBarProps) {
  const [model, setModel] = useState(MODELS[0]);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [activeIdx, setActiveIdx] = useState(0);

  useEffect(() => {
    api.settings.get("selected_model").then((item) => {
      if (item?.value && MODELS.includes(item.value)) {
        setModel(item.value);
      }
    }).catch(() => {});
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open]);

  const handleDropdownKey = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") { setOpen(false); return; }
    if (e.key === "ArrowDown") { e.preventDefault(); setActiveIdx((i) => (i + 1) % MODELS.length); }
    if (e.key === "ArrowUp") { e.preventDefault(); setActiveIdx((i) => (i - 1 + MODELS.length) % MODELS.length); }
    if (e.key === "Enter") {
      const m = MODELS[activeIdx];
      setModel(m);
      setOpen(false);
      api.settings.set("selected_model", m).catch(() => {});
    }
  };

  return (
    <div
      className="flex items-center h-12 px-3 gap-3 shrink-0"
      style={{ background: "var(--bg-surface)", borderBottom: "1px solid var(--border-default)" }}
    >
      <button
        onClick={onToggleSidebar}
        className="p-1.5 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] min-w-[44px] min-h-[44px] flex items-center justify-center"
        aria-label="切换侧边栏"
      >
        <PanelLeft size={18} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
      </button>

      <div className="flex items-center gap-2 pr-3" style={{ borderRight: "1px solid var(--border-subtle)" }}>
        <div
          className="w-6 h-6 rounded-md flex items-center justify-center"
          style={{ background: "var(--accent-primary)" }}
        >
          <span style={{ color: "#fff", fontSize: 13, fontWeight: 700 }}>A</span>
        </div>
        <span style={{ fontSize: 14, fontWeight: 600 }}>AI Writer</span>
      </div>

      <div className="flex items-center gap-2 min-w-0">
        <span style={{ color: "var(--text-secondary)", fontSize: 13 }} className="truncate">
          我的文稿 / <span style={{ color: "var(--text-primary)" }}>{fileName}</span>
        </span>
      </div>

      <div className="flex-1 flex items-center justify-center gap-2">
        <span
          className="inline-block w-2 h-2 rounded-full"
          style={{ background: "var(--accent-ai)", boxShadow: "0 0 0 3px rgba(139,92,246,0.15)" }}
        />
        <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
          <span style={{ color: "var(--accent-ai)", fontWeight: 500 }}>{model}</span> 在线
        </span>
      </div>

      <div className="relative" ref={dropdownRef}>
        <button
          onClick={() => { setOpen(!open); setActiveIdx(MODELS.indexOf(model)); }}
          onKeyDown={handleDropdownKey}
          aria-label="切换 AI 模型"
          aria-expanded={open}
          aria-haspopup="listbox"
          className="flex items-center justify-between gap-2 h-8 px-3 rounded transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
          style={{
            width: 180,
            background: "var(--bg-elevated)",
            border: "1px solid var(--border-default)",
            fontSize: 13,
          }}
        >
          <span>{model}</span>
          <ChevronDown size={14} strokeWidth={1.5} style={{ color: "var(--text-secondary)", transition: "transform 150ms ease", transform: open ? "rotate(180deg)" : "none" }} />
        </button>
        {open && (
          <div
            role="listbox"
            aria-label="选择模型"
            className="absolute right-0 top-10 w-48 rounded-lg overflow-hidden z-50"
            style={{
              background: "var(--bg-elevated)",
              border: "1px solid var(--border-default)",
              boxShadow: "0 4px 24px rgba(0,0,0,0.06)",
            }}
          >
            {MODELS.map((m, idx) => (
              <button
                key={m}
                role="option"
                aria-selected={m === model}
                onClick={() => { setModel(m); setOpen(false); api.settings.set("selected_model", m).catch(() => {}); }}
                className="flex items-center justify-between w-full h-9 px-3 transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
                style={{ fontSize: 13, background: idx === activeIdx ? "var(--bg-hover)" : undefined }}
              >
                <span>{m}</span>
                {m === model && <Check size={14} strokeWidth={1.5} style={{ color: "var(--accent-primary)" }} />}
              </button>
            ))}
          </div>
        )}
      </div>

      <button
        onClick={onPublish}
        aria-label="发布文章"
        className="flex items-center gap-1.5 h-8 px-3 rounded-md transition-all hover:opacity-90 active:scale-[0.97]"
        style={{
          background: "var(--accent-cta)",
          color: "#fff",
          fontSize: 13,
          fontWeight: 500,
        }}
      >
        <Send size={13} strokeWidth={1.5} />
        发布
      </button>

      <button
        onClick={onToggleTheme}
        className="p-1.5 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] min-w-[44px] min-h-[44px] flex items-center justify-center"
        aria-label="切换主题"
      >
        {isDark ? (
          <Sun size={18} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
        ) : (
          <Moon size={18} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
        )}
      </button>

      <button
        className="p-1.5 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] min-w-[44px] min-h-[44px] flex items-center justify-center"
        aria-label="设置"
      >
        <Settings size={18} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
      </button>
    </div>
  );
}
