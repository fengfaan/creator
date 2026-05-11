interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const body: ApiResponse<T> = await res.json();
  if (body.code !== 200 && body.code !== 201) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}

export interface FileNode {
  id: string;
  name: string;
  path: string;
  type: "file" | "folder";
  children?: FileNode[];
}

export interface SettingItem {
  key: string;
  value: string;
}

export interface AiGenerateRequest {
  action: "outline" | "draft" | "polish" | "continue";
  title: string;
  outline?: string;
  content: string;
}

export interface AiGenerateResponse {
  text: string;
  model: string;
}

export interface AiImageRequest {
  purpose: "hero" | "inline" | "cover";
  title: string;
  content: string;
  referenceText?: string;
}

export interface AiImageResponse {
  markdown: string;
  assetPath: string;
  url: string;
  filePath: string;
  prompt: string;
  alt: string;
  caption: string;
  model: string;
}

export interface AiCheckRequest {
  platform: "wechat" | "xhs";
  title: string;
  content: string;
}

export interface AiCheckIssue {
  level: "ok" | "warn" | "error";
  category: string;
  term: string;
  line: number;
  excerpt: string;
  suggestion: string;
}

export interface AiCheckResponse {
  summary: string;
  status: "ok" | "warn" | "error";
  aiReviewed: boolean;
  model: string;
  issues: AiCheckIssue[];
  aiReview: string;
}

export interface RpaPublishRequest {
  platform: "xhs";
  title: string;
  content: string;
  coverPath?: string;
}

export interface RpaJobResponse {
  jobId: string;
  platform: string;
  status: "QUEUED" | "RUNNING" | "WAITING_CONFIRMATION" | "PUBLISHING" | "PUBLISHED" | "FAILED";
  message: string;
}

export interface RpaLogEntry {
  sequence: number;
  time: string;
  level: "INFO" | "SUCCESS" | "WARN" | "ERROR";
  message: string;
}

export const api = {
  files: {
    list: () => request<FileNode[]>("/api/v1/files"),
    readContent: (path: string) =>
      request<string>(`/api/v1/files/content?path=${encodeURIComponent(path)}`),
    save: (path: string, content: string) =>
      request<string>("/api/v1/files/save", {
        method: "POST",
        body: JSON.stringify({ path, content }),
      }),
    create: (name: string, folder?: string) =>
      request<string>("/api/v1/files/create", {
        method: "POST",
        body: JSON.stringify({ name, folder: folder || "" }),
      }),
    delete: (path: string) =>
      request<void>(`/api/v1/files?path=${encodeURIComponent(path)}`, { method: "DELETE" }),
    rename: (oldPath: string, newName: string) =>
      request<string>("/api/v1/files/rename", {
        method: "POST",
        body: JSON.stringify({ oldPath, newName }),
      }),
  },
  settings: {
    list: () => request<SettingItem[]>("/api/v1/settings"),
    get: (key: string) =>
      request<SettingItem | null>(`/api/v1/settings/${key}`),
    set: (key: string, value: string) =>
      request<void>("/api/v1/settings", {
        method: "POST",
        body: JSON.stringify({ key, value }),
      }),
  },
  ai: {
    generate: (body: AiGenerateRequest) =>
      request<AiGenerateResponse>("/api/v1/ai/generate", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    generateImage: (body: AiImageRequest) =>
      request<AiImageResponse>("/api/v1/ai/image", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    check: (body: AiCheckRequest) =>
      request<AiCheckResponse>("/api/v1/ai/check", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },
  rpa: {
    start: (body: RpaPublishRequest) =>
      request<RpaJobResponse>("/api/v1/rpa/jobs", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    get: (jobId: string) =>
      request<RpaJobResponse>(`/api/v1/rpa/jobs/${encodeURIComponent(jobId)}`),
    confirm: (jobId: string) =>
      request<RpaJobResponse>(`/api/v1/rpa/jobs/${encodeURIComponent(jobId)}/confirm`, {
        method: "POST",
      }),
    logs: (jobId: string, after = 0) =>
      request<RpaLogEntry[]>(
        `/api/v1/rpa/jobs/${encodeURIComponent(jobId)}/logs?after=${after}`,
      ),
  },
};
