import { ChevronUp, ChevronDown, Terminal, Send, Loader2, Check } from "lucide-react";
import { useEffect, useRef, useState } from "react";

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

export function DistributionConsole() {
  const [expanded, setExpanded] = useState(true);
  const [logs, setLogs] = useState<LogLine[]>([
    { time: "12:00:00", level: "--", text: "等待发布指令..." },
  ]);
  const [wechatStatus, setWechatStatus] = useState<Status>("idle");
  const [xhsStatus, setXhsStatus] = useState<Status>("idle");
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [logs]);

  const now = () => new Date().toTimeString().slice(0, 8);

  const runPublish = async (
    platform: "wechat" | "xhs",
    setStatus: (s: Status) => void,
  ) => {
    setStatus("running");
    const name = platform === "wechat" ? "微信公众号" : "小红书";
    const steps: LogLine[] = [
      { time: now(), level: "OK", text: `正在连接 Chrome 浏览器...` },
      { time: now(), level: "OK", text: `已登录 ${name} 账号` },
      { time: now(), level: "OK", text: "正在上传封面图 (1/3)..." },
      { time: now(), level: "..", text: "图片压缩中, 请稍候" },
      { time: now(), level: "OK", text: "正在填写正文..." },
      { time: now(), level: "OK", text: `已发布到 ${name}!` },
    ];
    for (const s of steps) {
      await new Promise((r) => setTimeout(r, 600));
      setLogs((l) => [...l, { ...s, time: now() }]);
    }
    setStatus("success");
    setTimeout(() => setStatus("idle"), 2500);
  };

  return (
    <div
      className="shrink-0 flex flex-col"
      style={{
        background: "var(--bg-surface)",
        borderTop: "1px solid var(--border-default)",
      }}
    >
      <div
        className="flex items-center justify-between h-12 px-4 gap-3"
        style={{ borderBottom: expanded ? "1px solid var(--border-subtle)" : "none" }}
      >
        <div className="flex items-center gap-2 shrink-0">
          <Terminal size={15} strokeWidth={1.5} style={{ color: "var(--accent-primary)" }} />
          <span style={{ fontSize: 13, fontWeight: 600 }}>分发控制台</span>
          <span
            style={{
              fontSize: 11,
              color: "var(--text-muted)",
              marginLeft: 4,
              fontFamily: "var(--font-mono)",
            }}
          >
            {logs.length} logs
          </span>
        </div>

        <div className="flex items-center gap-2">
          <PublishButton
            status={wechatStatus}
            color="var(--accent-wechat)"
            label="发布到微信公众号"
            onClick={() => {
              setExpanded(true);
              runPublish("wechat", setWechatStatus);
            }}
          />
          <PublishButton
            status={xhsStatus}
            color="var(--accent-xhs)"
            label="发布到小红书"
            onClick={() => {
              setExpanded(true);
              runPublish("xhs", setXhsStatus);
            }}
          />
          <button
            onClick={() => setExpanded(!expanded)}
            className="ml-1 p-1.5 rounded-md transition-colors hover:bg-[var(--bg-hover)]"
            aria-label={expanded ? "收起日志" : "展开日志"}
          >
            {expanded ? (
              <ChevronDown size={16} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            ) : (
              <ChevronUp size={16} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            )}
          </button>
        </div>
      </div>

      {expanded && (
        <div className="flex flex-col" style={{ height: 200 }}>
          <div
            ref={logRef}
            className="flex-1 overflow-y-auto p-3 relative"
            style={{
              background: "#1A1A1A",
              fontFamily: "var(--font-mono)",
              fontSize: 12,
              lineHeight: 1.6,
              color: "#D1D5DB",
            }}
          >
            <div
              className="absolute top-0 left-0 right-0 pointer-events-none"
              style={{ height: 16, background: "linear-gradient(to bottom, #1A1A1A, transparent)" }}
            />
            {logs.map((l, i) => (
              <div key={i} className="flex gap-2">
                <span style={{ color: "#6B7280" }}>[{l.time}]</span>
                <span style={{ color: LEVEL_COLOR[l.level], fontWeight: 600 }}>{l.level}</span>
                <span>{l.text}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function PublishButton({
  status,
  color,
  label,
  onClick,
}: {
  status: Status;
  color: string;
  label: string;
  onClick: () => void;
}) {
  const isRunning = status === "running";
  const isSuccess = status === "success";
  const bg = isSuccess ? "var(--status-success)" : color;

  return (
    <button
      onClick={onClick}
      disabled={isRunning}
      className="flex items-center gap-1.5 h-9 px-3.5 rounded-md transition-all"
      style={{
        background: bg,
        color: "#fff",
        fontSize: 13,
        fontWeight: 500,
        opacity: isRunning ? 0.7 : 1,
        cursor: isRunning ? "not-allowed" : "pointer",
      }}
    >
      {isRunning ? (
        <Loader2 size={14} strokeWidth={2} className="animate-spin" />
      ) : isSuccess ? (
        <Check size={14} strokeWidth={2} />
      ) : (
        <Send size={14} strokeWidth={1.5} />
      )}
      {isRunning ? "发布中..." : isSuccess ? "发布成功" : label}
    </button>
  );
}
