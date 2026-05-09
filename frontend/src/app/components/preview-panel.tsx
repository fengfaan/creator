import { MessageCircle, BookOpen, ShieldCheck, Heart, MessageSquare, Star, AlertTriangle, X } from "lucide-react";
import { useState, useMemo } from "react";
import { ImageWithFallback } from "./figma/ImageWithFallback";

type Tab = "wechat" | "xhs";

interface Props {
  title: string;
  content: string;
}

export function PreviewPanel({ title, content }: Props) {
  const [tab, setTab] = useState<Tab>("wechat");
  const [showCheck, setShowCheck] = useState(false);

  const blocks = useMemo(() => parseMarkdown(content), [content]);
  const tags = useMemo(() => Array.from(content.matchAll(/#(\S+)/g)).map((m) => m[1]).slice(0, 6), [content]);

  return (
    <div className="flex-1 flex flex-col min-w-0 min-h-0" style={{ background: "var(--bg-deepest)" }}>
      {/* Tab bar */}
      <div
        className="flex items-center justify-between h-11 px-4 shrink-0"
        style={{ background: "var(--bg-surface)", borderBottom: "1px solid var(--border-default)" }}
      >
        <div className="flex items-center gap-1 h-full">
          <TabButton
            active={tab === "wechat"}
            color="var(--accent-wechat)"
            icon={<MessageCircle size={15} strokeWidth={1.5} />}
            label="微信公众号预览"
            onClick={() => setTab("wechat")}
          />
          <TabButton
            active={tab === "xhs"}
            color="var(--accent-xhs)"
            icon={<BookOpen size={15} strokeWidth={1.5} />}
            label="小红书预览"
            onClick={() => setTab("xhs")}
          />
        </div>

        <button
          onClick={() => setShowCheck(true)}
          className="flex items-center gap-1.5 h-7 px-3 rounded-md transition-colors"
          style={{
            background: "var(--accent-ai-light)",
            color: "var(--accent-ai)",
            fontSize: 12,
            fontWeight: 500,
          }}
        >
          <ShieldCheck size={14} strokeWidth={1.5} />
          AI 检查
        </button>
      </div>

      {/* Preview content */}
      <div className="flex-1 overflow-y-auto p-6 min-h-0">
        {showCheck && (
          <div
            className="mb-4 rounded-lg p-3 flex gap-3 mx-auto"
            style={{
              maxWidth: 420,
              background: "#FFFBEB",
              borderLeft: "3px solid var(--status-warning)",
            }}
          >
            <AlertTriangle size={18} strokeWidth={1.5} style={{ color: "var(--status-warning)", flexShrink: 0, marginTop: 2 }} />
            <div className="flex-1" style={{ fontSize: 12, color: "#78350F", lineHeight: 1.6 }}>
              <div style={{ fontWeight: 600, marginBottom: 4, color: "var(--status-warning)" }}>
                发现 2 处潜在违禁词
              </div>
              <div>第 12 行: "最好的" → 建议改为 "优秀的"</div>
              <div>第 28 行: "绝对有效" → 建议改为 "可能有效"</div>
              <div className="flex gap-2 mt-2">
                <button
                  className="px-2.5 h-6 rounded"
                  style={{ background: "var(--status-warning)", color: "#fff", fontSize: 11, fontWeight: 500 }}
                >
                  全部替换
                </button>
                <button
                  onClick={() => setShowCheck(false)}
                  className="px-2.5 h-6 rounded"
                  style={{ background: "transparent", color: "var(--status-warning)", fontSize: 11 }}
                >
                  忽略
                </button>
              </div>
            </div>
            <button onClick={() => setShowCheck(false)}>
              <X size={14} strokeWidth={1.5} style={{ color: "var(--status-warning)" }} />
            </button>
          </div>
        )}

        {tab === "wechat" ? <WechatPreview title={title} blocks={blocks} /> : <XhsPreview title={title} blocks={blocks} tags={tags} />}
      </div>
    </div>
  );
}

function TabButton({ active, color, icon, label, onClick }: any) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-1.5 h-full px-3 relative transition-colors"
      style={{
        color: active ? "var(--text-primary)" : "var(--text-secondary)",
        fontSize: 13,
        fontWeight: active ? 500 : 400,
      }}
    >
      <span style={{ color: active ? color : "var(--text-secondary)" }}>{icon}</span>
      {label}
      {active && (
        <span
          className="absolute bottom-0 left-3 right-3 rounded-t"
          style={{ height: 2, background: color }}
        />
      )}
    </button>
  );
}

function WechatPreview({ title, blocks }: { title: string; blocks: Block[] }) {
  return (
    <div
      className="mx-auto rounded-lg overflow-hidden"
      style={{
        maxWidth: 420,
        background: "#fff",
        border: "1px solid var(--border-default)",
        fontFamily: '"PingFang SC", "Microsoft YaHei", system-ui, sans-serif',
      }}
    >
      <div
        className="flex items-center gap-2 px-4 py-2"
        style={{ background: "#F7F7F7", borderBottom: "1px solid var(--border-subtle)", fontSize: 11, color: "#888" }}
      >
        <span
          className="w-4 h-4 rounded-sm flex items-center justify-center"
          style={{ background: "var(--accent-wechat)", color: "#fff", fontSize: 9, fontWeight: 700 }}
        >
          公
        </span>
        微信公众号预览
      </div>
      <div className="px-6 py-6">
        <h1 style={{ fontSize: 22, fontWeight: 700, lineHeight: 1.4, color: "#222", marginBottom: 12 }}>
          {title || "未命名文章"}
        </h1>
        <div className="flex items-center gap-2 mb-5" style={{ fontSize: 12, color: "#999" }}>
          <span>AI Writer</span>
          <span>·</span>
          <span>2026-05-09</span>
        </div>
        <div style={{ fontSize: 17, lineHeight: 1.85, color: "#333" }}>
          {blocks.map((b, i) => renderBlockWechat(b, i))}
        </div>
      </div>
    </div>
  );
}

function XhsPreview({ title, blocks, tags }: { title: string; blocks: Block[]; tags: string[] }) {
  return (
    <div
      className="mx-auto rounded-xl overflow-hidden"
      style={{
        maxWidth: 380,
        background: "#fff",
        border: "1px solid var(--border-default)",
        fontFamily: '"PingFang SC", system-ui, sans-serif',
      }}
    >
      <div className="relative" style={{ aspectRatio: "3/4", background: "var(--bg-hover)" }}>
        <ImageWithFallback
          src="https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600"
          alt="cover"
          className="w-full h-full object-cover"
        />
        <div
          className="absolute top-3 right-3 flex items-center gap-1 px-2 h-6 rounded-full"
          style={{ background: "rgba(0,0,0,0.5)", color: "#fff", fontSize: 11 }}
        >
          1/4
        </div>
      </div>
      <div className="px-4 py-4">
        <h2 style={{ fontSize: 16, fontWeight: 600, lineHeight: 1.4, color: "#222", marginBottom: 10 }}>
          {title || "分享一个超棒的小红书"}
        </h2>
        <div style={{ fontSize: 14, lineHeight: 1.75, color: "#444" }}>
          {blocks.map((b, i) => renderBlockXhs(b, i))}
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
        <div
          className="flex items-center gap-5 mt-4 pt-3"
          style={{ borderTop: "1px solid var(--border-subtle)", color: "#999", fontSize: 12 }}
        >
          <span className="flex items-center gap-1">
            <Heart size={14} strokeWidth={1.5} /> 1.2k
          </span>
          <span className="flex items-center gap-1">
            <MessageSquare size={14} strokeWidth={1.5} /> 88
          </span>
          <span className="flex items-center gap-1">
            <Star size={14} strokeWidth={1.5} /> 256
          </span>
        </div>
      </div>
    </div>
  );
}

type Block =
  | { type: "h"; level: number; text: string }
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
    else if (m[3]) parts.push(
      <code key={k++} style={{ background: "#F4F4F5", padding: "1px 5px", borderRadius: 3, fontSize: "0.9em", fontFamily: "var(--font-mono)" }}>{m[4]}</code>
    );
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));
  return <>{parts}</>;
}

function renderBlockWechat(b: Block, i: number) {
  switch (b.type) {
    case "h":
      return (
        <h3 key={i} style={{ fontSize: b.level === 1 ? 19 : 17, fontWeight: 700, margin: "20px 0 10px", color: "#222" }}>
          {inlineFmt(b.text)}
        </h3>
      );
    case "quote":
      return (
        <div key={i} style={{ borderLeft: "3px solid var(--accent-wechat)", background: "#F7F7F7", padding: "10px 14px", margin: "10px 0", color: "#666", fontSize: 15 }}>
          {inlineFmt(b.text)}
        </div>
      );
    case "li":
      return <div key={i} style={{ paddingLeft: 16, position: "relative", margin: "4px 0" }}>
        <span style={{ position: "absolute", left: 0 }}>·</span>
        {inlineFmt(b.text)}
      </div>;
    case "code":
      return (
        <pre key={i} style={{ background: "#F4F4F5", padding: 12, borderRadius: 6, fontSize: 13, fontFamily: "var(--font-mono)", overflowX: "auto", margin: "10px 0" }}>
          {b.text}
        </pre>
      );
    default:
      return <p key={i} style={{ margin: "10px 0" }}>{inlineFmt(b.text)}</p>;
  }
}

function renderBlockXhs(b: Block, i: number) {
  switch (b.type) {
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
