import {
  AlertTriangle,
  BookOpen,
  Check,
  CheckCircle2,
  Copy,
  ExternalLink,
  MessageCircle,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState, useCallback } from "react";

type PlatformId = "wechat" | "xhs";
type CheckLevel = "ok" | "warn" | "error";

interface Platform {
  id: PlatformId;
  name: string;
  desc: string;
  color: string;
  backendUrl: string;
  icon: React.ReactNode;
}

interface CheckItem {
  id: string;
  platform: PlatformId;
  level: CheckLevel;
  label: string;
  detail: string;
}

interface Rule {
  label: string;
  terms: string[];
}

interface Props {
  open: boolean;
  onClose: () => void;
  title: string;
  content: string;
}

const PLATFORMS: Platform[] = [
  {
    id: "wechat",
    name: "微信公众号",
    desc: "复制 HTML",
    color: "#07C160",
    backendUrl: "https://mp.weixin.qq.com/",
    icon: <MessageCircle size={16} strokeWidth={1.5} />,
  },
  {
    id: "xhs",
    name: "小红书",
    desc: "复制正文",
    color: "#FF2442",
    backendUrl: "https://creator.xiaohongshu.com/",
    icon: <BookOpen size={16} strokeWidth={1.5} />,
  },
];

const PLACEHOLDER_RULES: Rule[] = [
  { label: "绝对化表达", terms: ["最强", "最佳", "第一", "唯一", "顶级", "国家级", "100%", "百分百"] },
  { label: "医疗功效", terms: ["根治", "治愈", "药到病除", "无副作用"] },
  { label: "承诺保证", terms: ["保证有效", "稳赚", "永久有效", "零风险"] },
];

const LEVEL_STYLE: Record<CheckLevel, { color: string; bg: string; label: string }> = {
  ok: { color: "var(--status-success)", bg: "rgba(5,150,105,0.1)", label: "通过" },
  warn: { color: "var(--status-warning)", bg: "rgba(217,119,6,0.12)", label: "提醒" },
  error: { color: "var(--status-error)", bg: "rgba(220,38,38,0.1)", label: "需处理" },
};

export function PublishModal({ open, onClose, title, content }: Props) {
  const [selected, setSelected] = useState<PlatformId>("wechat");
  const [copied, setCopied] = useState<string>("");
  const modalRef = useRef<HTMLDivElement>(null);

  const draftTitle = title.trim() || "未命名文章";
  const normalizedContent = useMemo(() => stripDuplicatedTitle(content, draftTitle), [content, draftTitle]);
  const charCount = useMemo(() => countChars(normalizedContent), [normalizedContent]);
  const checks = useMemo(
    () => buildChecks(draftTitle, normalizedContent),
    [draftTitle, normalizedContent],
  );
  const selectedPlatform = PLATFORMS.find((p) => p.id === selected) || PLATFORMS[0];
  const selectedChecks = checks.filter((item) => item.platform === selected);
  const hasError = selectedChecks.some((item) => item.level === "error");

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === "Escape") {
      onClose();
      return;
    }
    if (e.key !== "Tab" || !modalRef.current) return;
    const focusable = modalRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  }, [onClose]);

  useEffect(() => {
    if (!open) return;
    document.addEventListener("keydown", handleKeyDown);
    setCopied("");
    requestAnimationFrame(() => {
      const first = modalRef.current?.querySelector<HTMLElement>('button:not([disabled])');
      first?.focus();
    });
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, handleKeyDown]);

  if (!open) return null;

  const copyFor = async (platform: PlatformId) => {
    const text = platform === "wechat"
      ? buildWechatHtml(draftTitle, normalizedContent)
      : buildXhsText(draftTitle, normalizedContent);
    await copyToClipboard(text);
    setCopied(platform);
    setTimeout(() => setCopied(""), 1800);
  };

  const openBackend = (platform: PlatformId) => {
    const target = PLATFORMS.find((p) => p.id === platform);
    if (!target) return;
    window.open(target.backendUrl, "_blank", "noopener,noreferrer");
  };

  const semiAuto = async () => {
    await copyFor(selected);
    openBackend(selected);
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
        className="rounded-lg overflow-hidden flex flex-col"
        style={{
          width: "min(760px, 100%)",
          maxHeight: "min(720px, 92vh)",
          background: "var(--bg-elevated)",
          boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
          animation: "modal-enter 200ms ease-out",
        }}
      >
        <div
          className="flex items-center justify-between px-5 h-12 shrink-0"
          style={{ borderBottom: "1px solid var(--border-subtle)" }}
        >
          <div className="flex items-center gap-2">
            <ExternalLink size={15} strokeWidth={1.5} style={{ color: "var(--accent-primary)" }} />
            <span id="publish-modal-title" style={{ fontSize: 14, fontWeight: 600 }}>
              发布前准备
            </span>
          </div>
          <button
            onClick={onClose}
            aria-label="关闭弹窗"
            className="p-1.5 rounded hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] min-w-[44px] min-h-[44px] flex items-center justify-center"
          >
            <X size={16} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4 min-h-0">
          <div
            className="grid gap-3 mb-4"
            style={{ gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}
          >
            <Metric label="标题" value={`${countChars(draftTitle)} 字`} detail={draftTitle} />
            <Metric label="正文" value={`${charCount} 字`} detail={`${normalizedContent.split(/\n/).filter(Boolean).length} 段`} />
          </div>

          <div className="grid grid-cols-2 gap-2 mb-4" role="tablist" aria-label="发布平台">
            {PLATFORMS.map((platform) => (
              <button
                key={platform.id}
                type="button"
                role="tab"
                aria-selected={selected === platform.id}
                onClick={() => setSelected(platform.id)}
                className="flex items-center gap-2.5 p-2.5 rounded-md transition-all text-left"
                style={{
                  background: selected === platform.id ? "var(--bg-surface)" : "var(--bg-deepest)",
                  border: selected === platform.id ? `1.5px solid ${platform.color}` : "1.5px solid var(--border-subtle)",
                }}
              >
                <span
                  className="w-8 h-8 rounded-md flex items-center justify-center shrink-0"
                  style={{
                    background: selected === platform.id ? platform.color : "var(--bg-hover)",
                    color: selected === platform.id ? "#fff" : platform.color,
                  }}
                >
                  {platform.icon}
                </span>
                <span className="min-w-0 flex-1">
                  <span style={{ display: "block", fontSize: 13, fontWeight: 600 }}>{platform.name}</span>
                  <span style={{ display: "block", fontSize: 11, color: "var(--text-secondary)" }}>{platform.desc}</span>
                </span>
                {selected === platform.id && <CheckCircle2 size={16} strokeWidth={2} style={{ color: platform.color }} />}
              </button>
            ))}
          </div>

          <section className="mb-4">
            <div className="flex items-center justify-between mb-2">
              <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>平台格式检查</span>
              <span style={{ fontSize: 11, color: hasError ? "var(--status-error)" : "var(--status-success)" }}>
                {hasError ? "存在需处理项" : "可进入半自动发布"}
              </span>
            </div>
            <div className="grid gap-2">
              {selectedChecks.map((item) => (
                <CheckRow key={item.id} item={item} />
              ))}
            </div>
          </section>

          <section>
            <div className="flex items-center justify-between mb-2">
              <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>导出与后台</span>
              <span style={{ fontSize: 11, color: "var(--text-muted)" }}>{selectedPlatform.name}</span>
            </div>
            <div className="grid gap-2" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
              <ActionButton
                icon={copied === selected ? <Check size={14} strokeWidth={2} /> : <Copy size={14} strokeWidth={1.5} />}
                label={selected === "wechat" ? "复制公众号 HTML" : "复制小红书正文"}
                onClick={() => copyFor(selected)}
                color={selectedPlatform.color}
              />
              <ActionButton
                icon={<ExternalLink size={14} strokeWidth={1.5} />}
                label={`打开${selectedPlatform.name}后台`}
                onClick={() => openBackend(selected)}
                color="var(--accent-primary)"
              />
            </div>
          </section>
        </div>

        <div
          className="flex items-center justify-between gap-3 px-5 py-3 shrink-0"
          style={{ borderTop: "1px solid var(--border-subtle)", background: "var(--bg-deepest)" }}
        >
          <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>
            {copied ? "已复制到剪贴板" : "半自动发布会复制内容并打开后台"}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="h-9 px-4 rounded-md transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
              style={{ fontSize: 13, color: "var(--text-secondary)" }}
            >
              取消
            </button>
            <button
              onClick={semiAuto}
              className="flex items-center gap-1.5 h-9 px-4 rounded-md transition-all active:scale-[0.97]"
              style={{
                background: selectedPlatform.color,
                color: "#fff",
                fontSize: 13,
                fontWeight: 600,
              }}
            >
              <Copy size={14} strokeWidth={1.5} />
              复制并打开
            </button>
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

function Metric({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div
      className="rounded-md p-3 min-w-0"
      style={{ background: "var(--bg-deepest)", border: "1px solid var(--border-subtle)" }}
    >
      <div style={{ fontSize: 11, color: "var(--text-secondary)", marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 16, fontWeight: 700 }}>{value}</div>
      <div className="truncate" style={{ fontSize: 11, color: "var(--text-muted)", marginTop: 4 }}>{detail}</div>
    </div>
  );
}

function CheckRow({ item }: { item: CheckItem }) {
  const style = LEVEL_STYLE[item.level];
  return (
    <div
      className="flex items-start gap-2 rounded-md px-3 py-2"
      style={{ background: "var(--bg-deepest)", border: "1px solid var(--border-subtle)" }}
    >
      <span
        className="w-6 h-6 rounded flex items-center justify-center shrink-0"
        style={{ color: style.color, background: style.bg }}
      >
        {item.level === "ok" ? <Check size={14} strokeWidth={2} /> : <AlertTriangle size={14} strokeWidth={1.7} />}
      </span>
      <span className="min-w-0 flex-1">
        <span style={{ display: "block", fontSize: 13, fontWeight: 600 }}>{item.label}</span>
        <span style={{ display: "block", fontSize: 12, color: "var(--text-secondary)" }}>{item.detail}</span>
      </span>
      <span style={{ fontSize: 11, color: style.color, fontWeight: 600 }}>{style.label}</span>
    </div>
  );
}

function ActionButton({
  icon,
  label,
  onClick,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
  color: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="h-10 px-3 rounded-md flex items-center justify-center gap-1.5 transition-all hover:opacity-90 active:scale-[0.98]"
      style={{ background: color, color: "#fff", fontSize: 13, fontWeight: 600 }}
    >
      {icon}
      {label}
    </button>
  );
}

function buildChecks(title: string, content: string): CheckItem[] {
  const titleLen = countChars(title);
  const bodyLen = countChars(content);
  return [
    lengthCheck("wechat-title", "wechat", "标题长度", titleLen, 64),
    lengthCheck("wechat-body", "wechat", "正文长度", bodyLen, 50000),
    ...sensitiveChecks("wechat", title, content),
    lengthCheck("xhs-title", "xhs", "标题长度", titleLen, 20),
    lengthCheck("xhs-body", "xhs", "正文长度", bodyLen, 1000),
    ...sensitiveChecks("xhs", title, content),
  ];
}

function lengthCheck(id: string, platform: PlatformId, label: string, current: number, max: number): CheckItem {
  const level: CheckLevel = current === 0 ? "error" : current > max ? "error" : current > max * 0.9 ? "warn" : "ok";
  return {
    id,
    platform,
    level,
    label,
    detail: `${current}/${max} 字`,
  };
}

function sensitiveChecks(platform: PlatformId, title: string, content: string): CheckItem[] {
  const text = `${title}\n${content}`;
  const matches = PLACEHOLDER_RULES
    .map((rule) => ({
      label: rule.label,
      hits: rule.terms.filter((term) => text.includes(term)),
    }))
    .filter((item) => item.hits.length > 0);
  if (matches.length === 0) {
    return [{
      id: `${platform}-sensitive-ok`,
      platform,
      level: "ok",
      label: "违禁词占位规则",
      detail: "未命中占位词",
    }];
  }
  return matches.map((match) => ({
    id: `${platform}-sensitive-${match.label}`,
    platform,
    level: "warn",
    label: match.label,
    detail: `命中: ${match.hits.join("、")}`,
  }));
}

function buildWechatHtml(title: string, content: string) {
  const body = markdownToHtml(content);
  return `<section style="font-size:16px;line-height:1.8;color:#1f2937;">\n<h1 style="font-size:24px;line-height:1.35;margin:0 0 20px;font-weight:700;">${escapeHtml(title)}</h1>\n${body}\n</section>`;
}

function buildXhsText(title: string, content: string) {
  const plain = content
    .replace(/^#{1,6}\s+/gm, "")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/\*([^*]+)\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
  return `${title}\n\n${plain}`.trim();
}

function markdownToHtml(markdown: string) {
  const lines = markdown.split("\n");
  const html: string[] = [];
  let inList = false;

  const closeList = () => {
    if (inList) {
      html.push("</ul>");
      inList = false;
    }
  };

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) {
      closeList();
      continue;
    }
    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      closeList();
      const level = heading[1].length + 1;
      html.push(`<h${level} style="font-size:${level === 2 ? 20 : 18}px;margin:22px 0 10px;">${inlineMarkdown(heading[2])}</h${level}>`);
      continue;
    }
    const bullet = line.match(/^[-*+]\s+(.+)$/);
    if (bullet) {
      if (!inList) {
        html.push('<ul style="padding-left:1.2em;margin:10px 0;">');
        inList = true;
      }
      html.push(`<li>${inlineMarkdown(bullet[1])}</li>`);
      continue;
    }
    closeList();
    html.push(`<p style="margin:0 0 14px;">${inlineMarkdown(line)}</p>`);
  }
  closeList();
  return html.join("\n");
}

function inlineMarkdown(text: string) {
  return escapeHtml(text)
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>");
}

function stripDuplicatedTitle(content: string, title: string) {
  const escaped = title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return content.replace(new RegExp(`^#\\s+${escaped}\\s*\\n+`, "i"), "").trim();
}

function countChars(text: string) {
  return Array.from(text.trim()).length;
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

async function copyToClipboard(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  document.body.removeChild(textarea);
}
