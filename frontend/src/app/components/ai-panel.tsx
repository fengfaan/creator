import { Sparkles, FileText, Expand, Wand2, ArrowRight, Languages, X, Loader2 } from "lucide-react";
import { useState } from "react";

interface AIAction {
  id: string;
  icon: React.ReactNode;
  label: string;
  desc: string;
  generate: (ctx: string) => string;
}

const ACTIONS: AIAction[] = [
  {
    id: "outline",
    icon: <FileText size={15} strokeWidth={1.5} />,
    label: "生成大纲",
    desc: "根据主题生成文章结构",
    generate: () =>
      `\n## 文章大纲\n\n1. **引言** — 抛出核心问题\n2. **现状分析** — 列举常见痛点\n3. **方法论** — 三步解决方案\n4. **案例展示** — 真实场景验证\n5. **总结升华** — 引导读者行动\n`,
  },
  {
    id: "expand",
    icon: <Expand size={15} strokeWidth={1.5} />,
    label: "扩写段落",
    desc: "扩展当前段落内容",
    generate: () =>
      `\n更进一步说，这个观点之所以重要，是因为它直接影响了内容的传播效率。在算法推荐的逻辑下，**前 3 秒**决定了用户是否会停留，而停留时长又反过来决定了流量分发的范围。因此，每一句话都应当为下一句话服务。\n`,
  },
  {
    id: "polish",
    icon: <Wand2 size={15} strokeWidth={1.5} />,
    label: "小红书润色",
    desc: "改写为小红书风格",
    generate: () =>
      `\n姐妹们！！！这个真的太香了 💗\n\n我跟你们说，自从用了这套方法后，我的笔记数据直接起飞 🚀\n\n✨ 重点划下来：\n· 选题要戳痛点\n· 标题要有数字\n· 封面一定要卷\n\n不点赞收藏血亏！#小红书运营\n`,
  },
  {
    id: "continue",
    icon: <ArrowRight size={15} strokeWidth={1.5} />,
    label: "AI 续写",
    desc: "续写下一段内容",
    generate: () =>
      `\n基于以上分析，我们可以得出一个更深层的结论：内容创作的本质，是把抽象的认知转化为具象的体验。读者感受到的不是文字本身，而是文字背后那个真实的「人」。这也是为什么个人 IP 在算法时代依然不可替代。\n`,
  },
  {
    id: "translate",
    icon: <Languages size={15} strokeWidth={1.5} />,
    label: "翻译为英文",
    desc: "翻译当前内容",
    generate: () =>
      `\n## English Translation\n\nWriting a viral note isn't magic — it's a method. A great topic must trigger emotion, target a clear audience, and align with the platform's tone. Pair that with a sharp headline formula (pain point + numbers + solution), and you've got the foundation of a hit.\n`,
  },
];

interface Props {
  onInsert: (text: string) => void;
  onClose: () => void;
}

export function AIPanel({ onInsert, onClose }: Props) {
  const [running, setRunning] = useState<string | null>(null);
  const [streamed, setStreamed] = useState<string>("");
  const [activeAction, setActiveAction] = useState<AIAction | null>(null);

  const runAction = async (action: AIAction) => {
    setRunning(action.id);
    setActiveAction(action);
    setStreamed("");
    const full = action.generate("");
    for (let i = 0; i < full.length; i++) {
      await new Promise((r) => setTimeout(r, 12));
      setStreamed((s) => s + full[i]);
    }
    setRunning(null);
  };

  const accept = () => {
    if (streamed) onInsert(streamed);
    setActiveAction(null);
    setStreamed("");
  };

  const reset = () => {
    setActiveAction(null);
    setStreamed("");
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
            AI 操作 · DeepSeek-V3
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
            <span style={{ fontSize: 13, fontWeight: 500 }}>{activeAction.label}</span>
            {running && <Loader2 size={12} className="animate-spin" style={{ color: "var(--accent-ai)" }} />}
          </div>

          <div
            className="flex-1 overflow-y-auto p-4"
            style={{
              background: "var(--bg-deepest)",
              fontFamily: "var(--font-mono)",
              fontSize: 13,
              lineHeight: 1.7,
              color: "var(--text-primary)",
              whiteSpace: "pre-wrap",
              minHeight: 180,
            }}
          >
            {streamed}
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
              disabled={!!running || !streamed}
              className="h-8 px-3.5 rounded-md transition-all"
              style={{
                background: "var(--accent-ai)",
                color: "#fff",
                fontSize: 12,
                fontWeight: 500,
                opacity: running || !streamed ? 0.5 : 1,
                cursor: running || !streamed ? "not-allowed" : "pointer",
              }}
            >
              插入到编辑器
            </button>
          </div>
        </>
      )}
    </div>
  );
}
