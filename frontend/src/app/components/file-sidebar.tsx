import { Search, Plus, FileText, Folder, FolderOpen, ChevronRight, ChevronDown } from "lucide-react";
import { useState } from "react";

interface FileNode {
  id: string;
  name: string;
  type: "file" | "folder";
  platform?: "wechat" | "xhs";
  children?: FileNode[];
}

const TREE: FileNode[] = [
  {
    id: "root",
    name: "我的文稿",
    type: "folder",
    children: [
      { id: "f1", name: "如何写好小红书.md", type: "file" },
      { id: "f2", name: "周报模板.md", type: "file" },
      {
        id: "fold1",
        name: "小红书文案",
        type: "folder",
        children: [
          { id: "f3", name: "第一篇.md", type: "file" },
          { id: "f4", name: "第二篇.md", type: "file" },
        ],
      },
    ],
  },
];

const PUBLISHED: FileNode[] = [
  { id: "p1", name: "春节特辑", type: "file", platform: "wechat" },
  { id: "p2", name: "美食攻略", type: "file", platform: "xhs" },
];

interface Props {
  activeId: string;
  onSelect: (id: string, name: string) => void;
}

export function FileSidebar({ activeId, onSelect }: Props) {
  const [query, setQuery] = useState("");
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ root: true, fold1: false });

  const renderNode = (node: FileNode, depth: number) => {
    const isExpanded = expanded[node.id];
    const isActive = activeId === node.id;
    const matches = !query || node.name.toLowerCase().includes(query.toLowerCase());

    if (node.type === "folder") {
      return (
        <div key={node.id}>
          <button
            onClick={() => setExpanded({ ...expanded, [node.id]: !isExpanded })}
            aria-label={`${isExpanded ? "折叠" : "展开"} ${node.name}`}
            className="flex items-center w-full h-9 gap-1 rounded transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
            style={{ paddingLeft: 8 + depth * 14, paddingRight: 8 }}
          >
            {isExpanded ? (
              <ChevronDown size={12} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} />
            ) : (
              <ChevronRight size={12} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} />
            )}
            {isExpanded ? (
              <FolderOpen size={14} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            ) : (
              <Folder size={14} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
            )}
            <span style={{ fontSize: 13, color: "var(--text-primary)" }}>{node.name}</span>
          </button>
          {isExpanded && node.children?.map((c) => renderNode(c, depth + 1))}
        </div>
      );
    }

    if (!matches) return null;

    return (
      <button
        key={node.id}
        onClick={() => onSelect(node.id, node.name)}
        className="flex items-center w-full h-9 gap-1.5 transition-colors relative hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)]"
        style={{
          paddingLeft: 8 + depth * 14 + 12,
          paddingRight: 8,
          background: isActive ? "var(--accent-primary-light)" : undefined,
        }}
      >
        {isActive && (
          <span
            className="absolute left-0 top-1 bottom-1 w-[3px] rounded-r"
            style={{ background: "var(--accent-primary)" }}
          />
        )}
        <FileText size={13} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
        <span
          style={{
            fontSize: 13,
            color: isActive ? "var(--accent-primary)" : "var(--text-primary)",
            fontWeight: isActive ? 500 : 400,
          }}
          className="truncate"
        >
          {node.name}
        </span>
      </button>
    );
  };

  return (
    <div
      className="flex flex-col h-full shrink-0"
      style={{ width: 240, background: "var(--bg-surface)", borderRight: "1px solid var(--border-default)" }}
    >
      <div className="p-3 space-y-2">
        <label className="flex items-center h-9 px-2.5 gap-2 rounded-md" style={{ background: "var(--bg-deepest)", border: "1px solid var(--border-subtle)" }}>
          <Search size={14} strokeWidth={1.5} style={{ color: "var(--text-muted)" }} aria-hidden="true" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="搜索文件..."
            aria-label="搜索文件"
            className="flex-1 bg-transparent outline-none"
            style={{ fontSize: 13, color: "var(--text-primary)" }}
          />
        </label>
        <button
          className="flex items-center justify-center w-full h-9 gap-1.5 rounded-md transition-colors hover:opacity-90 active:scale-[0.98]"
          style={{
            background: "var(--accent-primary-light)",
            color: "var(--accent-primary)",
            fontSize: 13,
            fontWeight: 500,
          }}
        >
          <Plus size={14} strokeWidth={1.5} />
          新建文档
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-2 pb-4">
        {TREE.map((n) => renderNode(n, 0))}

        <div className="mt-5 px-2 mb-1.5">
          <span style={{ fontSize: 12, color: "var(--text-muted)", letterSpacing: 1, fontWeight: 600 }}>
            已发布
          </span>
        </div>
        {PUBLISHED.map((p) => (
          <button
            key={p.id}
            className="flex items-center w-full h-9 px-2 gap-1.5 rounded transition-colors hover:bg-[var(--bg-hover)] active:bg-[var(--bg-pressed)] cursor-pointer text-left"
            aria-label={`${p.platform === "wechat" ? "微信" : "小红书"}已发布: ${p.name}`}
          >
            <span
              className="inline-flex items-center justify-center px-1.5 rounded"
              style={{
                background: p.platform === "wechat" ? "rgba(7,193,96,0.15)" : "rgba(255,36,66,0.15)",
                color: p.platform === "wechat" ? "var(--accent-wechat)" : "var(--accent-xhs)",
                fontSize: 10,
                fontWeight: 600,
                height: 16,
              }}
            >
              {p.platform === "wechat" ? "微信" : "小红书"}
            </span>
            <span style={{ fontSize: 13, color: "var(--text-primary)" }} className="truncate">
              {p.name}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}
