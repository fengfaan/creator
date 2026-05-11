import { ChevronUp, ChevronDown, Terminal, Send, Loader2, Check } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { api, type RpaLogEntry } from "../api";

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

interface Props {
  title: string;
  content: string;
}

export function DistributionConsole({ title, content }: Props) {
  const [expanded, setExpanded] = useState(true);
  const [coverPath, setCoverPath] = useState("");
  const [currentJobId, setCurrentJobId] = useState("");
  const [waitingConfirm, setWaitingConfirm] = useState(false);
  const [logs, setLogs] = useState<LogLine[]>([
    { time: "12:00:00", level: "--", text: "等待发布指令..." },
  ]);
  const [wechatStatus, setWechatStatus] = useState<Status>("idle");
  const [xhsStatus, setXhsStatus] = useState<Status>("idle");
  const logRef = useRef<HTMLDivElement>(null);
  const pollTimerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [logs]);

  useEffect(() => () => {
    if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
  }, []);

  const now = () => new Date().toTimeString().slice(0, 8);

  const appendLog = (line: LogLine) => setLogs((l) => [...l, line]);

  const appendRpaLogs = (items: RpaLogEntry[]) => {
    if (!items.length) return;
    setLogs((l) => [
      ...l,
      ...items.map((item) => ({
        time: item.time || now(),
        level: mapBackendLevel(item.level),
        text: item.message,
      })),
    ]);
  };

  const runWechat = () => {
    setExpanded(true);
    appendLog({ time: now(), level: "..", text: "MVP-3 先接小红书，公众号 RPA 暂不执行" });
    setWechatStatus("error");
    setTimeout(() => setWechatStatus("idle"), 1800);
  };

  const runXhs = async () => {
    setExpanded(true);
    if (!title.trim() || !content.trim()) {
      appendLog({ time: now(), level: "!!", text: "标题和正文不能为空" });
      setXhsStatus("error");
      setTimeout(() => setXhsStatus("idle"), 1800);
      return;
    }
    if (!coverPath.trim()) {
      appendLog({ time: now(), level: "!!", text: "小红书图文发布需要填写本机封面图路径" });
      setXhsStatus("error");
      setTimeout(() => setXhsStatus("idle"), 1800);
      return;
    }

    setXhsStatus("running");
    setWaitingConfirm(false);
    setCurrentJobId("");
    appendLog({ time: now(), level: "--", text: "正在创建小红书 RPA 任务..." });
    try {
      const job = await api.rpa.start({
        platform: "xhs",
        title: title.trim(),
        content,
        coverPath: coverPath.trim(),
      });
      setCurrentJobId(job.jobId);
      let lastSeq = 0;

      const poll = async () => {
        try {
          const newLogs = await api.rpa.logs(job.jobId, lastSeq);
          if (newLogs.length) {
            lastSeq = newLogs[newLogs.length - 1].sequence;
            appendRpaLogs(newLogs);
          }
          const current = await api.rpa.get(job.jobId);
          if (current.status === "FAILED") {
            setXhsStatus("error");
            setWaitingConfirm(false);
            appendLog({ time: now(), level: "!!", text: current.message || "RPA 执行失败" });
            setTimeout(() => setXhsStatus("idle"), 2500);
            return;
          }
          if (current.status === "WAITING_CONFIRMATION") {
            setXhsStatus("success");
            setWaitingConfirm(true);
            appendLog({ time: now(), level: "..", text: "请检查浏览器页面，确认无误后点击确认发布" });
            return;
          }
          if (current.status === "PUBLISHED") {
            setXhsStatus("success");
            setWaitingConfirm(false);
            setTimeout(() => setXhsStatus("idle"), 2500);
            return;
          }
          pollTimerRef.current = setTimeout(poll, 1000);
        } catch (e) {
          setXhsStatus("error");
          appendLog({
            time: now(),
            level: "!!",
            text: e instanceof Error ? e.message : "读取 RPA 日志失败",
          });
          setTimeout(() => setXhsStatus("idle"), 2500);
        }
      };

      poll();
    } catch (e) {
      setXhsStatus("error");
      appendLog({
        time: now(),
        level: "!!",
        text: e instanceof Error ? e.message : "RPA 任务创建失败",
      });
      setTimeout(() => setXhsStatus("idle"), 2500);
    }
  };

  const confirmPublish = async () => {
    if (!currentJobId) return;
    setXhsStatus("running");
    setWaitingConfirm(false);
    appendLog({ time: now(), level: "..", text: "已人工确认，正在自动点击小红书发布按钮..." });
    try {
      await api.rpa.confirm(currentJobId);
      let lastSeq = 0;

      const poll = async () => {
        try {
          const newLogs = await api.rpa.logs(currentJobId, lastSeq);
          if (newLogs.length) {
            lastSeq = newLogs[newLogs.length - 1].sequence;
            appendRpaLogs(newLogs);
          }
          const current = await api.rpa.get(currentJobId);
          if (current.status === "FAILED") {
            setXhsStatus("error");
            appendLog({ time: now(), level: "!!", text: current.message || "自动确认发布失败" });
            setTimeout(() => setXhsStatus("idle"), 2500);
            return;
          }
          if (current.status === "PUBLISHED") {
            setXhsStatus("success");
            appendLog({ time: now(), level: "OK", text: "确认发布流程完成" });
            setCurrentJobId("");
            setTimeout(() => setXhsStatus("idle"), 2500);
            return;
          }
          pollTimerRef.current = setTimeout(poll, 1000);
        } catch (e) {
          setXhsStatus("error");
          appendLog({
            time: now(),
            level: "!!",
            text: e instanceof Error ? e.message : "读取确认发布日志失败",
          });
          setTimeout(() => setXhsStatus("idle"), 2500);
        }
      };

      poll();
    } catch (e) {
      setXhsStatus("error");
      setWaitingConfirm(true);
      appendLog({
        time: now(),
        level: "!!",
        text: e instanceof Error ? e.message : "确认发布失败",
      });
      setTimeout(() => setXhsStatus("idle"), 2500);
    }
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

        <div className="flex items-center gap-2 min-w-0">
          <label className="flex items-center gap-2 min-w-0" style={{ fontSize: 12, color: "var(--text-secondary)" }}>
            <span className="shrink-0">封面</span>
            <input
              value={coverPath}
              onChange={(e) => setCoverPath(e.target.value)}
              placeholder="/Users/.../cover.jpg"
              className="h-8 w-52 px-2 rounded-md outline-none"
              style={{
                background: "var(--bg-deepest)",
                border: "1px solid var(--border-subtle)",
                color: "var(--text-primary)",
                fontSize: 12,
              }}
            />
          </label>
          <PublishButton
            status={wechatStatus}
            color="var(--accent-wechat)"
            label="发布到微信公众号"
            onClick={runWechat}
          />
          <PublishButton
            status={xhsStatus}
            color="var(--accent-xhs)"
            label="发布到小红书"
            onClick={runXhs}
          />
          {waitingConfirm && (
            <button
              onClick={confirmPublish}
              className="flex items-center gap-1.5 h-9 px-3.5 rounded-md transition-all"
              style={{
                background: "var(--status-warning)",
                color: "#fff",
                fontSize: 13,
                fontWeight: 600,
              }}
            >
              <Check size={14} strokeWidth={2} />
              确认发布
            </button>
          )}
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

function mapBackendLevel(level: RpaLogEntry["level"]): LogLine["level"] {
  if (level === "SUCCESS") return "OK";
  if (level === "ERROR") return "!!";
  if (level === "WARN") return "..";
  return "--";
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
      {isRunning ? "执行中..." : isSuccess ? "待确认" : label}
    </button>
  );
}
