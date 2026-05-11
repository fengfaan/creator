import { AlertTriangle, BookOpen, CheckCircle2, Clock3, FileText, Heart, Loader2, MessageCircle, MessageSquare, ShieldCheck, Star } from "lucide-react";
import { useMemo, useState } from "react";
import { api, type AiCheckResponse } from "../api";
import { ImageWithFallback } from "./figma/ImageWithFallback";

type PlatformId = "wechat" | "xhs";
type ContextTab = "preview" | "check" | "versions";

interface PlatformDrafts {
  wechat: string;
  xhs: string;
}

interface Props {
  title: string;
  content: string;
  activePlatform: PlatformId;
  outline: string;
  platformDrafts: PlatformDrafts;
}

const PLATFORM_META: Record<PlatformId, { label: string; color: string; icon: React.ReactNode }> = {
  wechat: { label: "公众号稿", color: "var(--accent-wechat)", icon: <MessageCircle size={14} strokeWidth={1.5} /> },
  xhs: { label: "小红书稿", color: "var(--accent-xhs)", icon: <BookOpen size={14} strokeWidth={1.5} /> },
};

export function PreviewPanel({ title, content, activePlatform, outline, platformDrafts }: Props) {
  const [tab, setTab] = useState<ContextTab>("preview");
  const [checkResult, setCheckResult] = useState<AiCheckResponse | null>(null);
  const [checkError, setCheckError] = useState("");
  const [checking, setChecking] = useState(false);
  const blocks = useMemo(() => parseMarkdown(content), [content]);
  const tags = useMemo(() => Array.from(content.matchAll(/#(\S+)/g)).map((m) => m[1]).slice(0, 6), [content]);
  const meta = PLATFORM_META[activePlatform];

  const runCheck = async () => {
    setTab("check");
    setChecking(true);
    setCheckError("");
    try {
      const result = await api.ai.check({ platform: activePlatform, title, content });
      setCheckResult(result);
    } catch (err) {
      setCheckResult(null);
      setCheckError(err instanceof Error ? err.message : "AI 检查失败");
    } finally {
      setChecking(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-w-0 min-h-0" style={{ background: "var(--bg-deepest)" }}>
      <div
        className="flex items-center justify-between h-11 px-4 shrink-0"
        style={{ background: "var(--bg-surface)", borderBottom: "1px solid var(--border-default)" }}
      >
        <div className="flex items-center gap-1 h-full" role="tablist" aria-label="右侧上下文">
          <ContextTabButton active={tab === "preview"} label="预览" onClick={() => setTab("preview")} />
          <ContextTabButton active={tab === "check"} label="AI 检查" onClick={runCheck} loading={checking} />
          <ContextTabButton active={tab === "versions"} label="版本" onClick={() => setTab("versions")} />
        </div>
        <button
          onClick={runCheck}
          disabled={checking}
          className="flex items-center gap-1.5 h-7 px-3 rounded-md transition-colors"
          style={{
            background: "var(--accent-ai-light)",
            color: "var(--accent-ai)",
            fontSize: 12,
            fontWeight: 500,
            opacity: checking ? 0.75 : 1,
          }}
        >
          {checking ? <Loader2 size={14} strokeWidth={1.5} className="animate-spin" /> : <ShieldCheck size={14} strokeWidth={1.5} />}
          检查当前稿
        </button>
      </div>

      <div className="flex items-center gap-2 h-9 px-4 shrink-0" style={{ borderBottom: "1px solid var(--border-subtle)", fontSize: 12, color: "var(--text-secondary)" }}>
        <span style={{ color: meta.color }}>{meta.icon}</span>
        <span>当前上下文：{meta.label} · 大纲 v3</span>
      </div>

      <div className="flex-1 overflow-y-auto p-6 min-h-0">
        {tab === "preview" && (
          activePlatform === "wechat"
            ? <WechatPreview title={title} blocks={blocks} />
            : <XhsPreview title={title} blocks={blocks} tags={tags} />
        )}
        {tab === "check" && (
          <CheckPanel result={checkResult} error={checkError} loading={checking} platform={activePlatform} />
        )}
        {tab === "versions" && (
          <VersionsPanel outline={outline} drafts={platformDrafts} activePlatform={activePlatform} />
        )}
      </div>
    </div>
  );
}

function ContextTabButton({ active, label, loading, onClick }: { active: boolean; label: string; loading?: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className="flex items-center gap-1.5 h-full px-3 relative transition-colors"
      style={{
        color: active ? "var(--text-primary)" : "var(--text-secondary)",
        fontSize: 13,
        fontWeight: active ? 600 : 500,
      }}
    >
      {loading && <Loader2 size={12} className="animate-spin" />}
      {label}
      {active && <span className="absolute bottom-0 left-3 right-3 rounded-t" style={{ height: 2, background: "var(--accent-primary)" }} />}
    </button>
  );
}

function CheckPanel({
  result,
  error,
  loading,
  platform,
}: {
  result: AiCheckResponse | null;
  error: string;
  loading: boolean;
  platform: PlatformId;
}) {
  const isOk = result?.status === "ok" && !error;
  const color = error ? "var(--status-error)" : isOk ? "var(--status-success)" : "var(--status-warning)";
  const bg = error ? "#FEF2F2" : isOk ? "#ECFDF5" : "#FFFBEB";
  const textColor = error ? "#7F1D1D" : isOk ? "#064E3B" : "#78350F";
  return (
    <div className="mx-auto" style={{ maxWidth: 440 }}>
      <div
        className="rounded-md p-4"
        style={{ background: bg, borderLeft: `3px solid ${color}`, color: textColor }}
      >
        <div className="flex items-start gap-3">
          {loading ? (
            <Loader2 size={18} strokeWidth={1.5} className="animate-spin" style={{ color, flexShrink: 0, marginTop: 2 }} />
          ) : isOk ? (
            <CheckCircle2 size={18} strokeWidth={1.5} style={{ color, flexShrink: 0, marginTop: 2 }} />
          ) : (
            <AlertTriangle size={18} strokeWidth={1.5} style={{ color, flexShrink: 0, marginTop: 2 }} />
          )}
          <div className="flex-1 min-w-0" style={{ fontSize: 12, lineHeight: 1.65 }}>
            <div style={{ fontWeight: 700, marginBottom: 4, color }}>
              {loading ? "正在检查当前稿" : error || result?.summary || `等待检查${PLATFORM_META[platform].label}`}
            </div>
            {!loading && result && (
              <>
                <div style={{ marginBottom: result.issues.length > 0 ? 8 : 0 }}>
                  {platform === "xhs" && result.riskScore != null
                    ? `风控评分：${result.riskScore} · ${result.riskLevel || "低风险"} · `
                    : ""}
                  {result.aiReviewed ? `模型：${result.model || "当前模型"}` : "仅完成本地规则检查"}
                </div>
                {result.issues.slice(0, 6).map((issue, index) => (
                  <div key={`${issue.term}-${issue.line}-${index}`} style={{ marginTop: 4 }}>
                    [{issue.category}] 第 {issue.line} 行: "{issue.term}" · {issue.suggestion}
                  </div>
                ))}
                {result.aiReview && (
                  <div className="mt-3 whitespace-pre-wrap" style={{ borderTop: "1px solid rgba(0,0,0,0.08)", paddingTop: 10 }}>
                    {result.aiReview}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function VersionsPanel({ outline, drafts, activePlatform }: { outline: string; drafts: PlatformDrafts; activePlatform: PlatformId }) {
  const rows = [
    { id: "outline", label: "大纲", desc: `${lineCount(outline)} 行`, color: "var(--accent-ai)", active: false },
    { id: "wechat", label: "公众号稿", desc: `${charCount(drafts.wechat)} 字`, color: "var(--accent-wechat)", active: activePlatform === "wechat" },
    { id: "xhs", label: "小红书稿", desc: drafts.xhs.trim() ? `${charCount(drafts.xhs)} 字` : "待生成", color: "var(--accent-xhs)", active: activePlatform === "xhs" },
  ];
  return (
    <div className="mx-auto grid gap-2" style={{ maxWidth: 440 }}>
      {rows.map((row) => (
        <div
          key={row.id}
          className="rounded-md p-3 flex items-center gap-3"
          style={{ background: "var(--bg-surface)", border: row.active ? `1.5px solid ${row.color}` : "1px solid var(--border-subtle)" }}
        >
          <span className="w-8 h-8 rounded-md flex items-center justify-center" style={{ color: row.color, background: "var(--bg-deepest)" }}>
            {row.id === "outline" ? <FileText size={15} /> : <Clock3 size={15} />}
          </span>
          <span className="flex-1">
            <span style={{ display: "block", fontSize: 13, fontWeight: 700 }}>{row.label}</span>
            <span style={{ display: "block", fontSize: 12, color: "var(--text-secondary)" }}>{row.desc} · 自动保存</span>
          </span>
          {row.active && <span style={{ fontSize: 11, color: row.color, fontWeight: 700 }}>当前</span>}
        </div>
      ))}
    </div>
  );
}

function WechatPreview({ title, blocks }: { title: string; blocks: Block[] }) {
  const heroImage = blocks.find((b): b is Extract<Block, { type: "image" }> => b.type === "image");
  const contentBlocks = heroImage ? blocks.filter((b) => b !== heroImage) : blocks;

  return (
    <div
      className="mx-auto rounded-md overflow-hidden"
      style={{
        maxWidth: 420,
        background: "#fff",
        border: "1px solid var(--border-default)",
        fontFamily: '"PingFang SC", "Microsoft YaHei", system-ui, sans-serif',
      }}
    >
      <div className="px-4 py-2" style={{ background: "#F7F7F7", borderBottom: "1px solid var(--border-subtle)", fontSize: 11, color: "#888" }}>
        预览：公众号稿 v2
      </div>
      {heroImage && (
        <ImageWithFallback
          src={imageSrc(heroImage.src)}
          alt={heroImage.alt}
          style={{ width: "100%", display: "block", aspectRatio: "16/9", objectFit: "cover" }}
        />
      )}
      <div className="px-6 py-6">
        <h1 style={{ fontSize: 22, fontWeight: 700, lineHeight: 1.4, color: "#222", marginBottom: 12 }}>
          {title || "未命名文章"}
        </h1>
        <div className="flex items-center gap-2 mb-5" style={{ fontSize: 12, color: "#999" }}>
          <span>AI Writer</span>
          <span>·</span>
          <span>2026-05-11</span>
        </div>
        <div style={{ fontSize: 17, lineHeight: 1.85, color: "#333" }}>
          {contentBlocks.map((b, i) => renderBlockWechat(b, i))}
        </div>
      </div>
    </div>
  );
}

function XhsPreview({ title, blocks, tags }: { title: string; blocks: Block[]; tags: string[] }) {
  const coverImage = blocks.find((b): b is Extract<Block, { type: "image" }> => b.type === "image");
  const contentBlocks = coverImage ? blocks.filter((b) => b !== coverImage) : blocks;

  return (
    <div
      className="mx-auto rounded-md overflow-hidden"
      style={{
        maxWidth: 380,
        background: "#fff",
        border: "1px solid var(--border-default)",
        fontFamily: '"PingFang SC", system-ui, sans-serif',
      }}
    >
      <div className="px-4 py-2" style={{ background: "#FFF1F3", borderBottom: "1px solid #FFE1E7", fontSize: 11, color: "#B4233C" }}>
        预览：小红书稿 v1
      </div>
      <div className="relative" style={{ aspectRatio: "3/4", background: "var(--bg-hover)" }}>
        <ImageWithFallback
          src={coverImage ? imageSrc(coverImage.src) : "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600"}
          alt={coverImage?.alt || "cover"}
          className="w-full h-full object-cover"
        />
      </div>
      <div className="px-4 py-4">
        <h2 style={{ fontSize: 16, fontWeight: 700, lineHeight: 1.4, color: "#222", marginBottom: 10 }}>
          {title || "分享一个超棒的小红书"}
        </h2>
        <div style={{ fontSize: 14, lineHeight: 1.75, color: "#444" }}>
          {contentBlocks.map((b, i) => renderBlockXhs(b, i))}
        </div>
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            {tags.map((t, i) => (
              <span key={`${t}-${i}`} style={{ color: "var(--accent-xhs)", fontSize: 13 }}>
                #{t}
              </span>
            ))}
          </div>
        )}
        <div className="flex items-center gap-5 mt-4 pt-3" style={{ borderTop: "1px solid var(--border-subtle)", color: "#999", fontSize: 12 }}>
          <span className="flex items-center gap-1"><Heart size={14} strokeWidth={1.5} /> 1.2k</span>
          <span className="flex items-center gap-1"><MessageSquare size={14} strokeWidth={1.5} /> 88</span>
          <span className="flex items-center gap-1"><Star size={14} strokeWidth={1.5} /> 256</span>
        </div>
      </div>
    </div>
  );
}

type Block =
  | { type: "h"; level: number; text: string }
  | { type: "image"; alt: string; src: string }
  | { type: "p"; text: string }
  | { type: "quote"; text: string }
  | { type: "li"; text: string }
  | { type: "code"; text: string };

function parseMarkdown(content: string): Block[] {
  const out: Block[] = [];
  const lines = content.split("\n");
  let inCode = false;
  let codeBuf: string[] = [];
  for (const line of lines) {
    if (/^```/.test(line)) {
      if (inCode) {
        out.push({ type: "code", text: codeBuf.join("\n") });
        codeBuf = [];
        inCode = false;
      } else {
        inCode = true;
      }
      continue;
    }
    if (inCode) {
      codeBuf.push(line);
      continue;
    }
    const image = line.match(/^!\[([^\]]*)\]\(([^)]+)\)/);
    if (image) {
      out.push({ type: "image", alt: image[1], src: image[2] });
      continue;
    }
    const h = line.match(/^(#{1,6})\s+(.*)/);
    if (h) {
      out.push({ type: "h", level: h[1].length, text: h[2] });
      continue;
    }
    if (/^>\s/.test(line)) {
      out.push({ type: "quote", text: line.replace(/^>\s/, "") });
      continue;
    }
    if (/^([-*+]|\d+\.)\s/.test(line)) {
      out.push({ type: "li", text: line.replace(/^([-*+]|\d+\.)\s/, "") });
      continue;
    }
    if (line.trim() === "") continue;
    out.push({ type: "p", text: line });
  }
  return out;
}

function inlineFmt(text: string): React.ReactNode {
  const parts: (string | JSX.Element)[] = [];
  const regex = /(\*\*([^*]+)\*\*)|(`([^`]+)`)/g;
  let last = 0, m: RegExpExecArray | null, k = 0;
  while ((m = regex.exec(text))) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    if (m[1]) parts.push(<strong key={k++}>{m[2]}</strong>);
    else if (m[3]) parts.push(<code key={k++} style={{ background: "#F4F4F5", padding: "1px 5px", borderRadius: 3, fontSize: "0.9em", fontFamily: "var(--font-mono)" }}>{m[4]}</code>);
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));
  return <>{parts}</>;
}

function renderBlockWechat(b: Block, i: number) {
  switch (b.type) {
    case "image":
      return <ImageWithFallback key={i} src={imageSrc(b.src)} alt={b.alt} style={{ width: "100%", borderRadius: 6, display: "block", margin: "16px 0" }} />;
    case "h":
      return <h3 key={i} style={{ fontSize: b.level === 1 ? 19 : 17, fontWeight: 700, margin: "20px 0 10px", color: "#222" }}>{inlineFmt(b.text)}</h3>;
    case "quote":
      return <div key={i} style={{ borderLeft: "3px solid var(--accent-wechat)", background: "#F7F7F7", padding: "10px 14px", margin: "10px 0", color: "#666", fontSize: 15 }}>{inlineFmt(b.text)}</div>;
    case "li":
      return <div key={i} style={{ paddingLeft: 16, position: "relative", margin: "4px 0" }}><span style={{ position: "absolute", left: 0 }}>·</span>{inlineFmt(b.text)}</div>;
    case "code":
      return <pre key={i} style={{ background: "#F4F4F5", padding: 12, borderRadius: 6, fontSize: 13, fontFamily: "var(--font-mono)", overflowX: "auto", margin: "10px 0" }}>{b.text}</pre>;
    default:
      return <p key={i} style={{ margin: "10px 0" }}>{inlineFmt(b.text)}</p>;
  }
}

function renderBlockXhs(b: Block, i: number) {
  switch (b.type) {
    case "image":
      return <ImageWithFallback key={i} src={imageSrc(b.src)} alt={b.alt} style={{ width: "100%", borderRadius: 8, display: "block", margin: "10px 0" }} />;
    case "h":
      return <div key={i} style={{ fontWeight: 700, margin: "10px 0 6px", color: "#222", fontSize: 14 }}>{inlineFmt(b.text)}</div>;
    case "quote":
    case "li":
      return <div key={i} style={{ margin: "4px 0" }}>· {inlineFmt(b.text)}</div>;
    case "code":
      return <pre key={i} style={{ background: "#F4F4F5", padding: 8, borderRadius: 4, fontSize: 12, fontFamily: "var(--font-mono)", overflowX: "auto" }}>{b.text}</pre>;
    default:
      return <p key={i} style={{ margin: "6px 0" }}>{inlineFmt(b.text)}</p>;
  }
}

function lineCount(text: string) {
  return text.trim() ? text.trim().split(/\n/).length : 0;
}

function charCount(text: string) {
  return text.replace(/\s/g, "").length;
}

function imageSrc(src: string) {
  if (/^(https?:|data:|\/api\/)/.test(src)) return src;
  if (src.startsWith("assets/")) return `/api/v1/assets/${src.slice("assets/".length)}`;
  return src;
}
