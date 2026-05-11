import { Cpu, KeyRound, Link2, Loader2, Save, X } from "lucide-react";
import { useEffect, useState } from "react";
import { api } from "../api";

const DEFAULT_BASE_URL = "https://api.deepseek.com";
const DEFAULT_MODEL = "deepseek-chat";
const DEFAULT_IMAGE_BASE_URL = "https://image.pollinations.ai";
const DEFAULT_IMAGE_MODEL = "sana";

interface SettingsModalProps {
  open: boolean;
  onClose: () => void;
  onSaved: (model: string) => void;
}

export function SettingsModal({ open, onClose, onSaved }: SettingsModalProps) {
  const [apiKey, setApiKey] = useState("");
  const [baseUrl, setBaseUrl] = useState(DEFAULT_BASE_URL);
  const [model, setModel] = useState(DEFAULT_MODEL);
  const [imageApiKey, setImageApiKey] = useState("");
  const [imageBaseUrl, setImageBaseUrl] = useState(DEFAULT_IMAGE_BASE_URL);
  const [imageModel, setImageModel] = useState(DEFAULT_IMAGE_MODEL);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    setStatus("");
    setError("");
    Promise.all([
      api.settings.get("ai_api_key"),
      api.settings.get("ai_base_url"),
      api.settings.get("selected_model"),
      api.settings.get("image_api_key"),
      api.settings.get("image_base_url"),
      api.settings.get("image_model"),
    ])
      .then(([keyItem, baseItem, modelItem, imageKeyItem, imageBaseItem, imageModelItem]) => {
        setApiKey(keyItem?.value || "");
        setBaseUrl(baseItem?.value || DEFAULT_BASE_URL);
        setModel(modelItem?.value || DEFAULT_MODEL);
        setImageApiKey(imageKeyItem?.value || "");
        setImageBaseUrl(imageBaseItem?.value || DEFAULT_IMAGE_BASE_URL);
        setImageModel(imageModelItem?.value || DEFAULT_IMAGE_MODEL);
      })
      .catch((e) => setError(e instanceof Error ? e.message : "设置加载失败"))
      .finally(() => setLoading(false));
  }, [open]);

  if (!open) return null;

  const save = async () => {
    setSaving(true);
    setError("");
    setStatus("");
    const nextBaseUrl = baseUrl.trim() || DEFAULT_BASE_URL;
    const nextModel = model.trim() || DEFAULT_MODEL;
    const nextImageBaseUrl = imageBaseUrl.trim() || DEFAULT_IMAGE_BASE_URL;
    const nextImageModel = imageModel.trim() || DEFAULT_IMAGE_MODEL;
    try {
      await Promise.all([
        api.settings.set("ai_api_key", apiKey.trim()),
        api.settings.set("ai_base_url", nextBaseUrl),
        api.settings.set("selected_model", nextModel),
        api.settings.set("image_api_key", imageApiKey.trim()),
        api.settings.set("image_base_url", nextImageBaseUrl),
        api.settings.set("image_model", nextImageModel),
      ]);
      setBaseUrl(nextBaseUrl);
      setModel(nextModel);
      setImageBaseUrl(nextImageBaseUrl);
      setImageModel(nextImageModel);
      setStatus("已保存");
      onSaved(nextModel);
    } catch (e) {
      setError(e instanceof Error ? e.message : "设置保存失败");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center px-4"
      style={{ background: "rgba(0,0,0,0.28)" }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="settings-title"
    >
      <div
        className="settings-dialog w-full overflow-hidden"
        style={{
          maxWidth: 520,
          background: "var(--bg-elevated)",
          color: "var(--text-primary)",
          border: "1px solid var(--border-default)",
          boxShadow: "0 18px 60px rgba(0,0,0,0.18)",
        }}
      >
        <div
          className="flex items-center justify-between h-12 px-4"
          style={{ borderBottom: "1px solid var(--border-subtle)", background: "var(--bg-surface)" }}
        >
          <h2 id="settings-title" style={{ fontSize: 14, fontWeight: 700 }}>
            设置
          </h2>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-md flex items-center justify-center transition-colors hover:bg-[var(--bg-hover)]"
            aria-label="关闭设置"
          >
            <X size={16} strokeWidth={1.5} style={{ color: "var(--text-secondary)" }} />
          </button>
        </div>

        <div className="p-4 space-y-4">
          {loading ? (
            <div className="h-44 flex items-center justify-center" style={{ color: "var(--text-secondary)", fontSize: 13 }}>
              <Loader2 size={16} className="animate-spin mr-2" />
              加载中
            </div>
          ) : (
            <>
              <SectionTitle label="写作模型" />
              <Field
                icon={<KeyRound size={15} strokeWidth={1.5} />}
                label="API Key"
                value={apiKey}
                onChange={setApiKey}
                type="password"
                placeholder="sk-..."
              />
              <Field
                icon={<Cpu size={15} strokeWidth={1.5} />}
                label="模型"
                value={model}
                onChange={setModel}
                placeholder="deepseek-chat"
              />
              <Field
                icon={<Link2 size={15} strokeWidth={1.5} />}
                label="Base URL"
                value={baseUrl}
                onChange={setBaseUrl}
                placeholder={DEFAULT_BASE_URL}
              />
              <SectionTitle label="图片模型" />
              <Field
                icon={<KeyRound size={15} strokeWidth={1.5} />}
                label="图片 API Key"
                value={imageApiKey}
                onChange={setImageApiKey}
                type="password"
                placeholder="同服务时可留空复用写作 Key"
              />
              <Field
                icon={<Cpu size={15} strokeWidth={1.5} />}
                label="图片模型"
                value={imageModel}
                onChange={setImageModel}
                placeholder={DEFAULT_IMAGE_MODEL}
              />
              <Field
                icon={<Link2 size={15} strokeWidth={1.5} />}
                label="图片 Base URL"
                value={imageBaseUrl}
                onChange={setImageBaseUrl}
                placeholder={DEFAULT_IMAGE_BASE_URL}
              />
            </>
          )}

          {(status || error) && (
            <div
              style={{
                fontSize: 12,
                color: error ? "var(--status-error)" : "var(--status-success)",
              }}
            >
              {error || status}
            </div>
          )}
        </div>

        <div
          className="flex items-center justify-end gap-2 px-4 py-3"
          style={{ borderTop: "1px solid var(--border-subtle)", background: "var(--bg-surface)" }}
        >
          <button
            onClick={onClose}
            className="h-9 px-3 rounded-md transition-colors hover:bg-[var(--bg-hover)]"
            style={{ fontSize: 13, color: "var(--text-secondary)" }}
          >
            取消
          </button>
          <button
            onClick={save}
            disabled={saving || loading}
            className="h-9 px-3 rounded-md flex items-center gap-1.5 transition-all hover:opacity-90"
            style={{
              background: "var(--accent-primary)",
              color: "#fff",
              fontSize: 13,
              fontWeight: 600,
              opacity: saving || loading ? 0.6 : 1,
            }}
          >
            {saving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} strokeWidth={1.5} />}
            保存
          </button>
        </div>
      </div>
    </div>
  );
}

function SectionTitle({ label }: { label: string }) {
  return (
    <div
      style={{
        fontSize: 12,
        fontWeight: 700,
        color: "var(--text-primary)",
        paddingTop: 2,
      }}
    >
      {label}
    </div>
  );
}

function Field({
  icon,
  label,
  value,
  onChange,
  placeholder,
  type = "text",
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 flex items-center gap-1.5" style={{ fontSize: 12, color: "var(--text-secondary)" }}>
        <span style={{ color: "var(--accent-primary)" }}>{icon}</span>
        {label}
      </span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        type={type}
        placeholder={placeholder}
        className="w-full h-10 px-3 rounded-md"
        style={{
          background: "var(--bg-deepest)",
          border: "1px solid var(--border-default)",
          color: "var(--text-primary)",
          fontSize: 13,
          fontFamily: label.includes("API Key") || label.includes("Base URL") || label.includes("模型")
            ? "var(--font-mono)"
            : undefined,
        }}
      />
    </label>
  );
}
