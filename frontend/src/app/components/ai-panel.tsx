import { Sparkles, FileText, Wand2, ArrowRight, X, Loader2, Image as ImageIcon, RectangleHorizontal, RectangleVertical, ScrollText } from "lucide-react";
import { useState } from "react";
import { api, type AiGenerateRequest, type AiImageRequest, type AiImageResponse } from "../api";

interface AIAction {
  id: AiGenerateRequest["action"] | "image-hero" | "image-inline" | "image-cover";
  kind: "text" | "image";
  purpose?: AiImageRequest["purpose"];
  icon: React.ReactNode;
  label: string;
  desc: string;
}

const ACTIONS: AIAction[] = [
  {
    id: "outline",
    kind: "text",
    icon: <FileText size={15} strokeWidth={1.5} />,
    label: "生成大纲",
    desc: "保存到独立大纲栏目",
  },
  {
    id: "draft",
    kind: "text",
    icon: <ScrollText size={15} strokeWidth={1.5} />,
    label: "生成正文",
    desc: "基于大纲扩写完整正文",
  },
  {
    id: "polish",
    kind: "text",
    icon: <Wand2 size={15} strokeWidth={1.5} />,
    label: "润色",
    desc: "优化表达和行文节奏",
  },
  {
    id: "continue",
    kind: "text",
    icon: <ArrowRight size={15} strokeWidth={1.5} />,
    label: "AI 续写",
    desc: "续写下一段内容",
  },
  {
    id: "image-hero",
    kind: "image",
    purpose: "hero",
    icon: <RectangleHorizontal size={15} strokeWidth={1.5} />,
    label: "生成首图",
    desc: "横版首图，适合文章开头",
  },
  {
    id: "image-inline",
    kind: "image",
    purpose: "inline",
    icon: <ImageIcon size={15} strokeWidth={1.5} />,
    label: "生成配图",
    desc: "结合正文生成段落配图",
  },
  {
    id: "image-cover",
    kind: "image",
    purpose: "cover",
    icon: <RectangleVertical size={15} strokeWidth={1.5} />,
    label: "生成封面",
    desc: "竖版封面，适合小红书",
  },
];

interface Props {
  title: string;
  outline: string;
  content: string;
  onInsert: (text: string) => void;
  onSaveOutline: (text: string) => void;
  onReplace: (text: string) => void;
  onClose: () => void;
}

export function AIPanel({ title, outline, content, onInsert, onSaveOutline, onReplace, onClose }: Props) {
  const [running, setRunning] = useState<string | null>(null);
  const [streamed, setStreamed] = useState<string>("");
  const [image, setImage] = useState<AiImageResponse | null>(null);
  const [activeAction, setActiveAction] = useState<AIAction | null>(null);
  const [error, setError] = useState("");
  const [model, setModel] = useState("");

  const runAction = async (action: AIAction) => {
    setRunning(action.id);
    setActiveAction(action);
    setStreamed("");
    setImage(null);
    setError("");
    try {
      if (action.kind === "image") {
        const result = await api.ai.generateImage({
          purpose: action.purpose || "inline",
          title,
          content,
        });
        setImage(result);
        setStreamed(result.markdown);
        setModel(result.model);
      } else {
        const result = await api.ai.generate({
          action: action.id,
          title,
          outline,
          content,
        });
        setStreamed(result.text);
        setModel(result.model);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "AI 生成失败");
    } finally {
      setRunning(null);
    }
  };

  const accept = () => {
    if (streamed && !error) {
      if (activeAction?.id === "outline") {
        onSaveOutline(streamed);
      } else if (activeAction?.id === "polish" || activeAction?.id === "draft") {
        onReplace(streamed);
      } else {
        onInsert(streamed);
      }
    }
    setActiveAction(null);
    setStreamed("");
    setImage(null);
    setError("");
  };

  const reset = () => {
    setActiveAction(null);
    setStreamed("");
    setImage(null);
    setError("");
    setRunning(null);
  };

  return (
    <div
      className="absolute bottom-4 right-4 left-4 rounded-xl overflow-hidden z-40 flex flex-col mx-auto"
      style={{
        maxWidth: 420,
        marginLeft: "auto",
        marginRight: 16,
        maxHeight: "calc(100% - 32px)",
        background: "var(--bg-elevated)",
        border: "1px solid var(--border-default)",
        boxShadow: "0 12px 40px rgba(0,0,0,0.12)",
      }}
    >
      <div
        className="flex items-center justify-between px-4 h-11 shrink-0"
        style={{ background: "var(--accent-ai-light)", borderBottom: "1px solid var(--border-subtle)" }}
      >
        <div className="flex items-center gap-2">
          <Sparkles size={16} strokeWidth={1.5} style={{ color: "var(--accent-ai)" }} />
          <span style={{ fontSize: 13, fontWeight: 600, color: "var(--accent-ai)" }}>
            AI 助写{model ? ` · ${model}` : ""}
          </span>
        </div>
        <button onClick={onClose} className="p-1 rounded hover:bg-white/50">
          <X size={14} strokeWidth={1.5} style={{ color: "var(--accent-ai)" }} />
        </button>
      </div>

      {!activeAction ? (
        <div className="overflow-y-auto p-2">
          {ACTIONS.map((a) => (
            <button
              key={a.id}
              onClick={() => runAction(a)}
              className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg transition-colors hover:bg-[var(--bg-hover)] text-left"
            >
              <div
                className="w-8 h-8 rounded-md flex items-center justify-center shrink-0"
                style={{ background: "var(--accent-ai-light)", color: "var(--accent-ai)" }}
              >
                {a.icon}
              </div>
              <div className="flex-1 min-w-0">
                <div style={{ fontSize: 13, fontWeight: 500, color: "var(--text-primary)" }}>
                  {a.label}
                </div>
                <div style={{ fontSize: 12, color: "var(--text-secondary)" }}>{a.desc}</div>
              </div>
              <ArrowRight size={14} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} />
            </button>
          ))}
        </div>
      ) : (
        <>
          <div className="px-4 py-2 flex items-center gap-2 shrink-0" style={{ borderBottom: "1px solid var(--border-subtle)" }}>
            <div
              className="w-6 h-6 rounded flex items-center justify-center"
              style={{ background: "var(--accent-ai-light)", color: "var(--accent-ai)" }}
            >
              {activeAction.icon}
            </div>
            <span className="min-w-0 flex-1">
              <span style={{ display: "block", fontSize: 13, fontWeight: 600 }}>{resultTitle(activeAction)}</span>
              <span style={{ display: "block", fontSize: 11, color: "var(--text-secondary)" }}>
                {resultHint(activeAction)}
              </span>
            </span>
            {running && <Loader2 size={12} className="animate-spin" style={{ color: "var(--accent-ai)" }} />}
          </div>

          <div
            className="flex-1 overflow-y-auto p-4"
            style={{
              background: "var(--bg-deepest)",
              fontFamily: image ? "inherit" : "var(--font-mono)",
              fontSize: 13,
              lineHeight: 1.7,
              color: "var(--text-primary)",
              whiteSpace: image ? "normal" : "pre-wrap",
              minHeight: 180,
            }}
          >
            {error ? (
              <span style={{ color: "var(--status-error)" }}>{error}</span>
            ) : image ? (
              <div className="space-y-3">
                <div
                  className="overflow-hidden"
                  style={{
                    border: "1px solid var(--border-subtle)",
                    background: "var(--bg-surface)",
                    borderRadius: 8,
                  }}
                >
                  <img src={image.url} alt={image.alt} className="w-full object-cover" />
                </div>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{image.alt}</div>
                {image.caption && (
                  <div style={{ fontSize: 12, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                    {image.caption}
                  </div>
                )}
                <div
                  style={{
                    fontFamily: "var(--font-mono)",
                    fontSize: 11,
                    color: "var(--text-muted)",
                    wordBreak: "break-all",
                  }}
                >
                  {image.assetPath}
                  <br />
                  本机路径：{image.filePath}
                </div>
              </div>
            ) : (
              <TextResult action={activeAction} text={streamed || (running ? "正在生成..." : "")} />
            )}
            {running && <span className="ai-caret" />}
          </div>

          <div className="flex items-center gap-2 px-3 py-2.5 shrink-0" style={{ borderTop: "1px solid var(--border-subtle)" }}>
            <button
              onClick={reset}
              className="h-8 px-3 rounded-md transition-colors hover:bg-[var(--bg-hover)]"
              style={{ fontSize: 12, color: "var(--text-secondary)" }}
            >
              返回
            </button>
            <div className="flex-1" />
            <button
              onClick={() => runAction(activeAction)}
              disabled={!!running}
              className="h-8 px-3 rounded-md transition-colors hover:bg-[var(--bg-hover)]"
              style={{
                fontSize: 12,
                color: "var(--text-secondary)",
                border: "1px solid var(--border-default)",
                opacity: running ? 0.5 : 1,
              }}
            >
              重新生成
            </button>
            <button
              onClick={accept}
              disabled={!!running || !streamed || !!error}
              className="h-8 px-3.5 rounded-md transition-all"
              style={{
                background: "var(--accent-ai)",
                color: "#fff",
                fontSize: 12,
                fontWeight: 500,
                opacity: running || !streamed || error ? 0.5 : 1,
                cursor: running || !streamed || error ? "not-allowed" : "pointer",
              }}
            >
              {acceptLabel(activeAction)}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function TextResult({ action, text }: { action: AIAction; text: string }) {
  if (!text) return null;
  const isPolish = action.id === "polish";
  const isDraft = action.id === "draft";
  return (
    <div>
      {(isPolish || isDraft) && (
        <div
          className="mb-3 rounded-md px-3 py-2"
          style={{
            fontFamily: "var(--font-sans)",
            fontSize: 12,
            lineHeight: 1.6,
            color: "var(--text-secondary)",
            background: "var(--bg-surface)",
            border: "1px solid var(--border-subtle)",
          }}
        >
          {isDraft ? "这是基于大纲扩写的完整正文，确认后会替换当前编辑区内容。" : "这是优化后的完整正文，确认后会替换当前编辑区内容。"}
        </div>
      )}
      <div
        className={action.id === "outline" ? "rounded-md px-3 py-2" : ""}
        style={{
          whiteSpace: "pre-wrap",
          background: action.id === "outline" ? "var(--bg-surface)" : "transparent",
          border: action.id === "outline" ? "1px solid var(--border-subtle)" : "none",
          borderLeft: action.id === "outline" ? "3px solid var(--accent-ai)" : undefined,
        }}
      >
        {text}
      </div>
    </div>
  );
}

function resultTitle(action: AIAction) {
  switch (action.id) {
    case "outline":
      return "大纲草稿";
    case "draft":
      return "基于大纲的正文";
    case "polish":
      return "优化后正文";
    case "continue":
      return "续写片段";
    case "image-hero":
      return "首图预览";
    case "image-cover":
      return "封面预览";
    default:
      return "配图预览";
  }
}

function resultHint(action: AIAction) {
  switch (action.id) {
    case "outline":
      return "确认后保存到大纲栏目，正文不会被改动";
    case "draft":
      return "依据大纲扩写，确认后替换当前正文";
    case "polish":
      return "用于替换当前正文，不会追加到末尾";
    case "continue":
      return "用于衔接下文，确认后插入到光标位置";
    case "image-hero":
      return "确认后插入横版首图 Markdown";
    case "image-cover":
      return "确认后插入竖版封面 Markdown";
    default:
      return "确认后插入配图 Markdown";
  }
}

function acceptLabel(action: AIAction) {
  switch (action.id) {
    case "outline":
      return "保存大纲";
    case "draft":
      return "替换正文";
    case "polish":
      return "替换正文";
    case "continue":
      return "插入续写";
    default:
      return "插入图片";
  }
}
