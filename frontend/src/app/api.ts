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
    list: () => request<{ key: string; value: string }[]>("/api/v1/settings"),
    get: (key: string) =>
      request<{ key: string; value: string } | null>(`/api/v1/settings/${key}`),
    set: (key: string, value: string) =>
      request<void>("/api/v1/settings", {
        method: "POST",
        body: JSON.stringify({ key, value }),
      }),
  },
};
