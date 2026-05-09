import { X, Send, Loader2, Check, Terminal, MessageCircle, BookOpen, Twitter, Facebook, Linkedin, Rss, Instagram, CheckCircle2 } from "lucide-react";
import { useEffect, useMemo, useRef, useState, useCallback } from "react";

type Status = "idle" | "running" | "success" | "error";

interface LogLine {
  time: string;
  level: "OK" | "!!" | ".." | "--";
  text: string;
}

const LEVEL_COLOR: Record<LogLine["level"], string> = {
  OK: "#10B981",
  "!!": "#EF4444",
  "..": "#F59E0B",
  "--": "#38BDF8",
};

interface Platform {
  id: string;
  name: string;
  desc: string;
  color: string;
  icon: React.ReactNode;
  connected: boolean;
}

const PLATFORMS: Platform[] = [
  { id: "wechat", name: "微信公众号", desc: "图文消息", color: "#07C160", icon: <MessageCircle size={16} strokeWidth={1.5} />, connected: true },
  { id: "xhs", name: "小红书", desc: "笔记卡片", color: "#FF2442", icon: <BookOpen size={16} strokeWidth={1.5} />, connected: true },
  { id: "twitter", name: "Twitter / X", desc: "线程发布", color: "#1DA1F2", icon: <Twitter size={16} strokeWidth={1.5} />, connected: true },
  { id: "facebook", name: "Facebook", desc: "Page 推送", color: "#1877F2", icon: <Facebook size={16} strokeWidth={1.5} />, connected: false },
  { id: "linkedin", name: "LinkedIn", desc: "动态文章", color: "#0A66C2", icon: <Linkedin size={16} strokeWidth={1.5} />, connected: false },
  { id: "instagram", name: "Instagram", desc: "图文 / Reels", color: "#E1306C", icon: <Instagram size={16} strokeWidth={1.5} />, connected: false },
  { id: "rss", name: "个人博客", desc: "RSS / Hugo", color: "#F26522", icon: <Rss size={16} strokeWidth={1.5} />, connected: true },
];

interface Props {
  open: boolean;
  onClose: () => void;
  title: string;
}

export function PublishModal({ open, onClose, title }: Props) {
  const [selected, setSelected] = useState<Record<string, boolean>>({ wechat: true });
  const [status, setStatus] = useState<Status>("idle");
  const [logs, setLogs] = useState<LogLine[]>([]);
  const logRef = useRef<HTMLDivElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  const selectedList = useMemo(
    () => PLATFORMS.filter((p) => selected[p.id] && p.connected),
    [selected],
  );

  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [logs]);

  // Focus trap + Escape
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === "Escape") { onClose(); return; }
    if (e.key !== "Tab" || !modalRef.current) return;
    const focusable = modalRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  }, [onClose]);

  useEffect(() => {
    if (!open) return;
    document.addEventListener("keydown", handleKeyDown);
    // Auto-focus first focusable element
    requestAnimationFrame(() => {
      const first = modalRef.current?.querySelector<HTMLElement>('button:not([disabled])');
      first?.focus();
    });
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, handleKeyDown]);

  if (!open) return null;

  const now = () => new Date().toTimeString().slice(0, 8);

  const togglePlatform = (id: string) => {
    if (status === "running") return;
    const p = PLATFORMS.find((x) => x.id === id);
    if (!p || !p.connected) return;
    setSelected((s) => ({ ...s, [id]: !s[id] }));
  };

  const run = async () => {
    if (selectedList.length === 0 || status === "running") return;
    setStatus("running");
    setLogs([{ time: now(), level: "--", text: `开始发布: ${title}` }]);

    for (const p of selectedList) {
      const steps: Omit<LogLine, "time">[] = [
        { level: "OK", text: `[${p.name}] 正在连接...` },
        { level: "OK", text: `[${p.name}] 已登录账号` },
        { level: "..", text: `[${p.name}] 上传媒体资源...` },
        { level: "OK", text: `[${p.name}] 填写正文完成` },
        { level: "OK", text: `[${p.name}] 发布成功 ✓` },
      ];
      for (const s of steps) {
        await new Promise((r) => setTimeout(r, 350));
        setLogs((l) => [...l, { ...s, time: now() }]);
      }
    }
    setStatus("success");
  };

  const reset = () => {
    setStatus("idle");
    setLogs([]);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: "rgba(0,0,0,0.45)" }}
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="publish-modal-title"
    >
      <div
        ref={modalRef}
        onClick={(e) => e.stopPropagation()}
        className="rounded-xl overflow-hidden flex flex-col"
        style={{
          width: "min(600px, 100%)",
          maxHeight: "min(680px, 92vh)",
          background: "var(--bg-elevated)",
          boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
          animation: "modal-enter 200ms ease-out",
        }}
      >
        {/* Header */}
        <div
          className="flex items-center justify-between px-5 h-12 shrink-0"
          style={{ borderBottom: "1px solid var(--border-subtle)" }}
        >
          <div className="flex items-center gap-2">
            <Send size={15} strokeWidth={1.5} style={{ color: "var(--accent-primary)" }} />
            <span id="publish-modal-title" style={{ fontSize: 14, fontWeight: 600 }}>发布到平台</span>
          </div>
          <button onClick={onClose} aria-label="关闭弹窗" className="p-1.5 rounded hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] min-w-[44px] min-h-[44px] flex items-center justify-center">
            <X size={16} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
          </button>
        </div>

        {/* Body — scrollable */}
        <div className="flex-1 overflow-y-auto px-5 py-4 min-h-0">
          <label style={{ fontSize: 12, color: "var(--text-secondary)", marginBottom: 6, display: "block" }}>
            当前文档
          </label>
          <div
            className="px-3 py-2 rounded-md mb-4 truncate"
            style={{ background: "var(--bg-deepest)", fontSize: 13, fontWeight: 500 }}
            role="status"
          >
            {title || "未命名文章"}
          </div>

          <div className="flex items-center justify-between mb-2">
            <span id="platform-label" style={{ fontSize: 12, color: "var(--text-secondary)" }}>选择发布平台</span>
            <button
              onClick={() => {
                const allOn = PLATFORMS.filter((p) => p.connected).every((p) => selected[p.id]);
                const next: Record<string, boolean> = {};
                if (!allOn) PLATFORMS.forEach((p) => { if (p.connected) next[p.id] = true; });
                setSelected(next);
              }}
              aria-label="全选已连接平台"
              style={{ fontSize: 11, color: "var(--accent-primary)" }}
              className="hover:underline"
            >
              全选已连接
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2" role="group" aria-labelledby="platform-label">
            {PLATFORMS.map((p) => (
              <PlatformCard
                key={p.id}
                platform={p}
                active={!!selected[p.id]}
                onClick={() => togglePlatform(p.id)}
              />
            ))}
          </div>

          {/* Logs */}
          {logs.length > 0 && (
            <div className="mt-4 rounded-lg overflow-hidden flex flex-col" style={{ background: "#1A1A1A" }}>
              <div
                className="flex items-center gap-2 px-3 h-7 shrink-0"
                style={{ borderBottom: "1px solid #2A2A2A" }}
              >
                <Terminal size={12} strokeWidth={1.5} style={{ color: "#9CA3AF" }} />
                <span style={{ fontSize: 11, color: "#9CA3AF", fontFamily: "var(--font-mono)" }}>
                  执行日志 · {logs.length}
                </span>
              </div>
              <div
                ref={logRef}
                className="overflow-y-auto p-3"
                role="log"
                aria-live="polite"
                style={{
                  fontFamily: "var(--font-mono)",
                  fontSize: 12,
                  lineHeight: 1.6,
                  color: "#D1D5DB",
                  maxHeight: 200,
                }}
              >
                {logs.map((l, i) => (
                  <div key={i} className="flex gap-2">
                    <span style={{ color: "#8B8FA3" }}>[{l.time}]</span>
                    <span style={{ color: LEVEL_COLOR[l.level], fontWeight: 600 }}>{l.level}</span>
                    <span>{l.text}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          className="flex items-center justify-between px-5 h-14 shrink-0"
          style={{ borderTop: "1px solid var(--border-subtle)", background: "var(--bg-deepest)" }}
        >
          <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>
            已选 <span style={{ color: "var(--text-primary)", fontWeight: 600 }}>{selectedList.length}</span> 个平台
          </span>
          <div className="flex items-center gap-2">
            {status === "success" ? (
              <>
                <button
                  onClick={reset}
                  className="h-9 px-4 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
                  style={{ fontSize: 13, color: "var(--text-secondary)" }}
                >
                  再次发布
                </button>
                <button
                  onClick={onClose}
                  className="flex items-center gap-1.5 h-9 px-4 rounded-md"
                  style={{ background: "var(--status-success)", color: "#fff", fontSize: 13, fontWeight: 500 }}
                >
                  <Check size={14} strokeWidth={2} />
                  完成
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={onClose}
                  disabled={status === "running"}
                  className="h-9 px-4 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
                  style={{
                    fontSize: 13,
                    color: "var(--text-secondary)",
                    opacity: status === "running" ? 0.5 : 1,
                  }}
                >
                  取消
                </button>
                <button
                  onClick={run}
                  disabled={selectedList.length === 0 || status === "running"}
                  className="flex items-center gap-1.5 h-9 px-4 rounded-md transition-all active:scale-[0.97]"
                  style={{
                    background: "var(--accent-cta)",
                    color: "#fff",
                    fontSize: 13,
                    fontWeight: 500,
                    opacity: selectedList.length === 0 || status === "running" ? 0.5 : 1,
                    cursor: selectedList.length === 0 || status === "running" ? "not-allowed" : "pointer",
                  }}
                >
                  {status === "running" ? (
                    <>
                      <Loader2 size={14} strokeWidth={2} className="animate-spin" />
                      发布中...
                    </>
                  ) : (
                    <>
                      <Send size={14} strokeWidth={1.5} />
                      开始发布
                    </>
                  )}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
      <style>{`
        @keyframes modal-enter {
          from { opacity: 0; transform: scale(0.96) translateY(8px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        @media (prefers-reduced-motion: reduce) {
          @keyframes modal-enter { from { opacity: 1; transform: none; } to { opacity: 1; transform: none; } }
        }
      `}</style>
    </div>
  );
}

function PlatformCard({
  platform,
  active,
  onClick,
}: {
  platform: Platform;
  active: boolean;
  onClick: () => void;
}) {
  const disabled = !platform.connected;
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={`${platform.name}${disabled ? "，未连接" : active ? "，已选择" : ""}`}
      className="flex items-center gap-2.5 p-2.5 rounded-lg transition-all text-left relative"
      style={{
        background: active ? "var(--bg-surface)" : "var(--bg-deepest)",
        border: active ? `1.5px solid ${platform.color}` : "1.5px solid var(--border-subtle)",
        opacity: disabled ? 0.55 : 1,
        cursor: disabled ? "not-allowed" : "pointer",
      }}
    >
      <div
        className="w-8 h-8 rounded-md flex items-center justify-center shrink-0"
        style={{
          background: active ? platform.color : "var(--bg-hover)",
          color: active ? "#fff" : platform.color,
        }}
      >
        {platform.icon}
      </div>
      <div className="min-w-0 flex-1">
        <div
          className="flex items-center gap-1"
          style={{ fontSize: 13, fontWeight: 500, color: "var(--text-primary)" }}
        >
          <span className="truncate">{platform.name}</span>
        </div>
        <div style={{ fontSize: 11, color: "var(--text-secondary)" }} className="truncate">
          {disabled ? "未连接" : platform.desc}
        </div>
      </div>
      {active && (
        <CheckCircle2
          size={16}
          strokeWidth={2}
          style={{ color: platform.color, flexShrink: 0 }}
        />
      )}
    </button>
  );
}
