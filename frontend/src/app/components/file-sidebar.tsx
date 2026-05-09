import { Search, Plus, FileText, Folder, FolderOpen, ChevronRight, ChevronDown } from "lucide-react";
import { useState, useEffect, useCallback } from "react";
import { api, FileNode } from "../api";

interface Props {
  activeId: string;
  activePath: string;
  onSelect: (id: string, name: string, path: string) => void;
}

export function FileSidebar({ activeId, activePath, onSelect }: Props) {
  const [query, setQuery] = useState("");
  const [tree, setTree] = useState<FileNode[]>([]);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ root: true });

  const loadTree = useCallback(async () => {
    try {
      const data = await api.files.list();
      setTree(data);
    } catch (e) {
      console.error("Failed to load file tree:", e);
    }
  }, []);

  useEffect(() => { loadTree(); }, [loadTree]);

  const handleCreate = async () => {
    const name = prompt("输入文件名（例如：新文章.md）");
    if (!name) return;
    try {
      const path = await api.files.create(name);
      await loadTree();
      const id = path.replace(/[^a-zA-Z0-9]/g, "_");
      onSelect(id, name, path);
    } catch (e) {
      console.error("Failed to create file:", e);
    }
  };

  const toggleExpand = (id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const matches = (node: FileNode): boolean =>
    !query || node.name.toLowerCase().includes(query.toLowerCase());

  const renderNode = (node: FileNode, depth: number) => {
    const isExpanded = expanded[node.id];
    const isActive = activeId === node.id || activePath === node.path;

    if (node.type === "folder") {
      return (
        <div key={node.id}>
          <button
            onClick={() => toggleExpand(node.id)}
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

    if (!matches(node)) return null;

    return (
      <button
        key={node.id}
        onClick={() => onSelect(node.id, node.name, node.path)}
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
          onClick={handleCreate}
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
        {tree.map((n) => renderNode(n, 0))}
      </div>
    </div>
  );
}
