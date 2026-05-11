import { Search, Plus, FileText, Folder, FolderOpen, ChevronRight, ChevronDown, X } from "lucide-react";
import { useState, useEffect, useCallback, useRef } from "react";
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
  const [createOpen, setCreateOpen] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [createError, setCreateError] = useState("");
  const [creating, setCreating] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const loadTree = useCallback(async () => {
    try {
      const data = await api.files.list();
      setTree(data);
    } catch (e) {
      console.error("Failed to load file tree:", e);
    }
  }, []);

  useEffect(() => { loadTree(); }, [loadTree]);

  useEffect(() => {
    if (!createOpen) return;
    requestAnimationFrame(() => inputRef.current?.focus());
  }, [createOpen]);

  const closeCreateDialog = () => {
    if (creating) return;
    setCreateOpen(false);
    setDraftName("");
    setCreateError("");
  };

  const handleCreate = async (e?: React.FormEvent) => {
    e?.preventDefault();
    const rawName = draftName.trim();
    if (!rawName) {
      setCreateError("请输入文件名");
      return;
    }
    const name = rawName.endsWith(".md") ? rawName : `${rawName}.md`;
    setCreating(true);
    setCreateError("");
    try {
      const path = await api.files.create(name);
      await loadTree();
      const id = path.replace(/[^a-zA-Z0-9]/g, "_");
      onSelect(id, name, path);
      setCreateOpen(false);
      setDraftName("");
    } catch (e) {
      console.error("Failed to create file:", e);
      setCreateError("创建失败，请稍后重试");
    } finally {
      setCreating(false);
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
          onClick={() => setCreateOpen(true)}
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

      {createOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ background: "rgba(0,0,0,0.38)" }}
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-file-title"
          onClick={closeCreateDialog}
        >
          <form
            onSubmit={handleCreate}
            onClick={(e) => e.stopPropagation()}
            className="w-full rounded-lg overflow-hidden"
            style={{
              maxWidth: 360,
              background: "var(--bg-elevated)",
              border: "1px solid var(--border-default)",
              boxShadow: "0 18px 48px rgba(0,0,0,0.18)",
            }}
          >
            <div
              className="flex items-center justify-between h-11 px-4"
              style={{ borderBottom: "1px solid var(--border-subtle)" }}
            >
              <div className="flex items-center gap-2">
                <FileText size={15} strokeWidth={1.5} style={{ color: "var(--accent-primary)" }} />
                <span id="create-file-title" style={{ fontSize: 14, fontWeight: 600 }}>
                  新建文档
                </span>
              </div>
              <button
                type="button"
                onClick={closeCreateDialog}
                aria-label="关闭"
                className="p-1.5 rounded-md hover:bg-[var(--bg-hover)] min-w-[36px] min-h-[36px] flex items-center justify-center"
              >
                <X size={15} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
              </button>
            </div>

            <div className="p-4">
              <label htmlFor="new-file-name" style={{ display: "block", fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 }}>
                文件名
              </label>
              <input
                ref={inputRef}
                id="new-file-name"
                value={draftName}
                onChange={(e) => {
                  setDraftName(e.target.value);
                  if (createError) setCreateError("");
                }}
                placeholder="例如：新文章.md"
                className="w-full h-10 px-3 rounded-md bg-transparent"
                style={{
                  border: "1px solid var(--border-default)",
                  color: "var(--text-primary)",
                  fontSize: 13,
                }}
              />
              {createError && (
                <div style={{ marginTop: 8, color: "var(--status-error)", fontSize: 12 }}>
                  {createError}
                </div>
              )}
            </div>

            <div
              className="flex items-center justify-end gap-2 px-4 py-3"
              style={{ borderTop: "1px solid var(--border-subtle)", background: "var(--bg-deepest)" }}
            >
              <button
                type="button"
                onClick={closeCreateDialog}
                disabled={creating}
                className="h-9 px-3 rounded-md hover:bg-[var(--bg-hover)]"
                style={{ color: "var(--text-secondary)", fontSize: 13, opacity: creating ? 0.5 : 1 }}
              >
                取消
              </button>
              <button
                type="submit"
                disabled={creating}
                className="h-9 px-3.5 rounded-md"
                style={{
                  background: "var(--accent-primary)",
                  color: "#fff",
                  fontSize: 13,
                  fontWeight: 500,
                  opacity: creating ? 0.7 : 1,
                }}
              >
                {creating ? "创建中..." : "创建"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
