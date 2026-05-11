import { useMemo, useRef, useState } from "react";
import { BookOpen, CheckCircle2, ChevronDown, FileText, Loader2, MessageCircle, RefreshCw, Sparkles, Wand2 } from "lucide-react";
import { AIPanel } from "./ai-panel";
import { api } from "../api";

type PlatformId = "wechat" | "xhs";

interface PlatformDrafts {
  wechat: string;
  xhs: string;
}

interface Props {
  title: string;
  setTitle: (v: string) => void;
  outline: string;
  setOutline: (v: string) => void;
  content: string;
  setContent: (v: string) => void;
  activePlatform: PlatformId;
  setActivePlatform: (platform: PlatformId) => void;
  platformDrafts: PlatformDrafts;
  setPlatformContent: (platform: PlatformId, content: string) => void;
  onCoverGenerated?: (filePath: string) => void;
}

const PLATFORM_META: Record<PlatformId, { label: string; color: string; icon: React.ReactNode; source: string }> = {
  wechat: {
    label: "公众号稿",
    color: "var(--accent-wechat)",
    icon: <MessageCircle size={14} strokeWidth={1.5} />,
    source: "公众号长文",
  },
  xhs: {
    label: "小红书稿",
    color: "var(--accent-xhs)",
    icon: <BookOpen size={14} strokeWidth={1.5} />,
    source: "小红书种草笔记",
  },
};

export function EditorPanel({
  title,
  setTitle,
  outline,
  setOutline,
  content,
  setContent,
  activePlatform,
  setActivePlatform,
  platformDrafts,
  setPlatformContent,
  onCoverGenerated,
}: Props) {
  const [showAI, setShowAI] = useState(false);
  const [outlineOpen, setOutlineOpen] = useState(false);
  const [generating, setGenerating] = useState<"draft" | "adapt" | null>(null);
  const [generateError, setGenerateError] = useState("");
  const taRef = useRef<HTMLTextAreaElement>(null);

  const lines = useMemo(() => content.split("\n"), [content]);
  const outlineLines = useMemo(() => outline.trim().split(/\n/).filter(Boolean).length, [outline]);
  const activeMeta = PLATFORM_META[activePlatform];
  const hasXhs = platformDrafts.xhs.trim().length > 0;

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
    requestAnimationFrame(() => {
      ta.focus();
      const pos = start + text.length;
      ta.setSelectionRange(pos, pos);
    });
  };

  const generateCurrentDraft = async () => {
    setGenerating("draft");
    setGenerateError("");
    try {
      const result = await api.ai.generate({
        action: "draft",
        platform: activePlatform === "xhs" ? "xhs" : undefined,
        title,
        outline,
        content: platformPrompt(activePlatform, content),
      });
      setContent(preserveImages(content, result.text));
    } catch (err) {
      setGenerateError(err instanceof Error ? err.message : "正文生成失败");
    } finally {
      setGenerating(null);
    }
  };

  const adaptWechatToXhs = async () => {
    setGenerating("adapt");
    setGenerateError("");
    try {
      const result = await api.ai.generate({
        action: "draft",
        platform: "xhs",
        title,
        outline,
        content: [
          "请把下面公众号稿改写成小红书稿，不要照搬公众号长文结构。",
          "小红书要求：标题更短，有开头钩子；正文分段短；保留关键信息；适合移动端阅读；结尾补充 3-6 个相关话题标签。",
          "",
          platformDrafts.wechat || content,
        ].join("\n"),
      });
      setActivePlatform("xhs");
      setPlatformContent("xhs", result.text);
    } catch (err) {
      setGenerateError(err instanceof Error ? err.message : "小红书改写失败");
    } finally {
      setGenerating(null);
    }
  };

  return (
    <div className="flex-1 flex flex-col min-w-0 min-h-0 relative" style={{ background: "var(--bg-surface)" }}>
      <div className="flex-1 overflow-y-auto min-h-0">
        <div className="mx-auto" style={{ maxWidth: 820, padding: "30px 32px 112px" }}>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="输入标题..."
            aria-label="文章标题"
            className="editor-input w-full bg-transparent border-0"
            style={{
              fontSize: 28,
              fontWeight: 700,
              lineHeight: 1.3,
              color: "var(--text-primary)",
            }}
          />

          <div className="mt-4 flex items-center gap-2" aria-label="写作阶段">
            {["1 大纲", "2 正文", "3 检查", "4 发布"].map((step, index) => (
              <span
                key={step}
                className="h-7 px-2.5 rounded-md flex items-center"
                style={{
                  fontSize: 12,
                  fontWeight: index === 1 ? 600 : 500,
                  color: index === 1 ? "var(--accent-primary)" : "var(--text-secondary)",
                  background: index === 1 ? "var(--accent-primary-light)" : "var(--bg-deepest)",
                  border: "1px solid var(--border-subtle)",
                }}
              >
                {step}
              </span>
            ))}
          </div>

          <div
            className="mt-4 rounded-md"
            style={{ border: "1px solid var(--border-subtle)", background: "var(--bg-deepest)" }}
          >
            <button
              type="button"
              onClick={() => setOutlineOpen((v) => !v)}
              className="w-full h-10 px-3 flex items-center gap-2 text-left"
              aria-expanded={outlineOpen}
            >
              <FileText size={15} strokeWidth={1.5} style={{ color: "var(--accent-ai)" }} />
              <span style={{ fontSize: 13, fontWeight: 600, color: "var(--text-primary)" }}>大纲 v3 已保存</span>
              <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>
                · {outlineLines || 0} 行 · 来源检查通过
              </span>
              <span className="flex-1" />
              <span style={{ fontSize: 12, color: "var(--text-secondary)" }}>{outlineOpen ? "收起大纲" : "展开大纲"}</span>
              <ChevronDown
                size={14}
                strokeWidth={1.5}
                style={{
                  color: "var(--text-muted)",
                  transform: outlineOpen ? "rotate(180deg)" : "rotate(0deg)",
                  transition: "transform 160ms ease",
                }}
              />
            </button>
            {outlineOpen && (
              <div style={{ borderTop: "1px solid var(--border-subtle)" }}>
                <textarea
                  value={outline}
                  onChange={(e) => setOutline(e.target.value)}
                  placeholder="先生成或手写大纲。正文生成会优先参考这里。"
                  className="editor-input w-full bg-transparent resize-y"
                  style={{
                    minHeight: 120,
                    maxHeight: 260,
                    padding: "12px 14px",
                    fontFamily: "var(--font-mono)",
                    fontSize: 13,
                    lineHeight: 1.65,
                    color: "var(--text-primary)",
                  }}
                />
              </div>
            )}
          </div>

          <div className="mt-4 flex items-center justify-between gap-3 platform-toolbar">
            <div className="flex items-center gap-1 rounded-md p-1" style={{ background: "var(--bg-deepest)", border: "1px solid var(--border-subtle)" }}>
              {(Object.keys(PLATFORM_META) as PlatformId[]).map((platform) => {
                const meta = PLATFORM_META[platform];
                const active = activePlatform === platform;
                const empty = !platformDrafts[platform].trim();
                return (
                  <button
                    key={platform}
                    type="button"
                    onClick={() => setActivePlatform(platform)}
                    className="h-8 px-3 rounded flex items-center gap-1.5 transition-colors"
                    style={{
                      background: active ? "var(--bg-surface)" : "transparent",
                      color: active ? "var(--text-primary)" : "var(--text-secondary)",
                      boxShadow: active ? "0 0 0 1px var(--border-subtle)" : "none",
                      fontSize: 13,
                      fontWeight: active ? 600 : 500,
                    }}
                  >
                    <span style={{ color: active ? meta.color : "var(--text-muted)" }}>{meta.icon}</span>
                    {meta.label}
                    {platform === "xhs" && empty && (
                      <span
                        className="ml-1 px-1.5 rounded"
                        style={{ background: "rgba(255,36,66,0.1)", color: "var(--accent-xhs)", fontSize: 10 }}
                      >
                        待生成
                      </span>
                    )}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center gap-2">
              <button className="editor-action" type="button" onClick={() => setShowAI(true)}>
                <Sparkles size={13} strokeWidth={1.5} />
                AI 优化
              </button>
              <button className="editor-action" type="button" onClick={adaptWechatToXhs} disabled={!!generating || (!platformDrafts.wechat.trim() && !content.trim())}>
                {generating === "adapt" ? <Loader2 size={13} className="animate-spin" /> : <Wand2 size={13} strokeWidth={1.5} />}
                改写为小红书
              </button>
              <button className="editor-primary-action" type="button" onClick={generateCurrentDraft} disabled={!!generating || !outline.trim()}>
                {generating === "draft" ? <Loader2 size={13} className="animate-spin" /> : <RefreshCw size={13} strokeWidth={1.5} />}
                {content.trim() ? "重新生成" : `生成${activeMeta.label}`}
              </button>
            </div>
          </div>

          <div className="mt-3 flex items-center gap-2" style={{ fontSize: 12, color: "var(--text-secondary)" }}>
            <CheckCircle2 size={13} strokeWidth={1.6} style={{ color: activeMeta.color }} />
            <span>来源：大纲 v3 · {activeMeta.source} · {activePlatform === "xhs" && !hasXhs ? "尚未生成独立稿" : "独立版本编辑中"}</span>
            {generateError && <span style={{ color: "var(--status-error)" }}>{generateError}</span>}
          </div>

          <div
            className="editor-field relative mt-3"
            style={{
              fontFamily: "var(--font-mono)",
              fontSize: 14,
              lineHeight: 1.7,
              background: "var(--bg-deepest)",
              border: "1px solid var(--border-subtle)",
              borderRadius: 6,
              overflow: "hidden",
            }}
          >
            <div
              aria-hidden
              className="absolute inset-0 pointer-events-none"
              style={{
                width: 44,
                paddingTop: 12,
                paddingBottom: 12,
                background: "var(--bg-surface)",
                borderRight: "1px solid var(--border-subtle)",
                color: "var(--text-muted)",
                overflow: "hidden",
              }}
            >
              {lines.map((_, i) => (
                <div key={i} style={{ height: "1.7em", textAlign: "right", paddingRight: 10, fontSize: 12 }}>
                  {i + 1}
                </div>
              ))}
            </div>
            <textarea
              ref={taRef}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              spellCheck={false}
              placeholder={activePlatform === "wechat" ? "在这里编辑公众号稿..." : "在这里编辑小红书稿，或点击“改写为小红书”生成独立版本..."}
              className="editor-input relative w-full bg-transparent resize-none"
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 14,
                lineHeight: 1.7,
                color: "var(--text-primary)",
                caretColor: "var(--accent-primary)",
                padding: "12px 12px 12px 56px",
                minHeight: 620,
              }}
              rows={Math.max(22, lines.length + 2)}
            />
          </div>
        </div>
      </div>

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
          title={title}
          outline={outline}
          content={platformPrompt(activePlatform, content)}
          platform={activePlatform}
          onInsert={(text) => {
            insertAtCursor(text);
            setShowAI(false);
          }}
          onSaveOutline={(text) => {
            setOutline(text);
            setShowAI(false);
          }}
          onReplace={(text) => {
            setContent(text);
            setShowAI(false);
            requestAnimationFrame(() => {
              const ta = taRef.current;
              if (ta) {
                ta.focus();
                ta.setSelectionRange(0, 0);
              }
            });
          }}
          onCoverGenerated={onCoverGenerated}
          onClose={() => setShowAI(false)}
        />
      )}
    </div>
  );
}

function platformPrompt(platform: PlatformId, content: string) {
  if (platform === "wechat") {
    return [
      "当前正在写微信公众号稿。请按公众号长文风格处理：结构完整、段落清晰、小标题明确、适合深度阅读。",
      "",
      content,
    ].join("\n");
  }
  return [
    "当前正在写小红书稿。请按小红书风格处理：标题短、开头有钩子、段落短、口语化、有行动建议，结尾可带话题标签。",
    "",
    content,
  ].join("\n");
}

const IMAGE_BLOCK_RE = /!\[[^\]]*\]\([^)]+\)(?:\s*\n> .+)*/g;

function preserveImages(oldContent: string, newContent: string): string {
  const oldImages = oldContent.match(IMAGE_BLOCK_RE);
  if (!oldImages || oldImages.length === 0) return newContent;
  const missing = oldImages.filter((img) => !newContent.includes(img.split("\n")[0]));
  if (missing.length === 0) return newContent;
  return newContent.trimEnd() + "\n\n" + missing.join("\n\n");
}
