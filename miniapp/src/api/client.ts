import { config } from "../config";
import { storage } from "../lib/storage";
import type { TokenResponse } from "./types";

const ACCESS_KEY = "zma.accessToken";
const REFRESH_KEY = "zma.refreshToken";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public errors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export const tokenStore = {
  get access() {
    return storage.get(ACCESS_KEY);
  },
  get refresh() {
    return storage.get(REFRESH_KEY);
  },
  save(t: Pick<TokenResponse, "accessToken" | "refreshToken">) {
    storage.set(ACCESS_KEY, t.accessToken);
    storage.set(REFRESH_KEY, t.refreshToken);
  },
  clear() {
    storage.remove(ACCESS_KEY);
    storage.remove(REFRESH_KEY);
  },
};

let onSessionExpired: (() => void) | null = null;
/** AuthProvider đăng ký để đăng nhập lại khi refresh thất bại. */
export function setSessionExpiredHandler(fn: (() => void) | null) {
  onSessionExpired = fn;
}

let refreshing: Promise<boolean> | null = null;

/** Single-flight: nhiều request 401 cùng lúc chỉ gọi /auth/refresh MỘT lần. */
export function refreshTokens(): Promise<boolean> {
  if (!refreshing) {
    refreshing = (async () => {
      const rt = tokenStore.refresh;
      if (!rt) return false;
      try {
        const res = await fetch(`${config.apiBaseUrl}/api/v1/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: rt }),
        });
        if (!res.ok) return false;
        tokenStore.save((await res.json()) as TokenResponse);
        return true;
      } catch {
        return false;
      }
    })().finally(() => {
      setTimeout(() => (refreshing = null), 0);
    });
  }
  return refreshing;
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Gửi kèm JWT (mặc định true nếu có token) */
  auth?: boolean;
  signal?: AbortSignal;
}

export async function api<T>(path: string, opts: RequestOptions = {}, retried = false): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  const isForm = typeof FormData !== "undefined" && opts.body instanceof FormData;
  if (opts.body !== undefined && !isForm) headers["Content-Type"] = "application/json";
  const token = tokenStore.access;
  if (opts.auth !== false && token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${config.apiBaseUrl}/api/v1${path}`, {
      method: opts.method ?? "GET",
      headers,
      body: opts.body === undefined ? undefined : isForm ? (opts.body as FormData) : JSON.stringify(opts.body),
      signal: opts.signal,
    });
  } catch (e) {
    if ((e as Error).name === "AbortError") throw e;
    throw new ApiError(0, "NETWORK_ERROR", "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.");
  }

  if (res.status === 401 && opts.auth !== false && !retried && tokenStore.refresh) {
    const ok = await refreshTokens();
    if (ok) return api<T>(path, opts, true);
    tokenStore.clear();
    onSessionExpired?.();
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? safeJson(text) : undefined;
  if (!res.ok) {
    const d = (data ?? {}) as { code?: string; message?: string; errors?: Record<string, string> };
    throw new ApiError(res.status, d.code ?? "HTTP_" + res.status, d.message ?? "Đã có lỗi xảy ra, vui lòng thử lại", d.errors);
  }
  return data as T;
}

function safeJson(t: string) {
  try {
    return JSON.parse(t);
  } catch {
    return undefined;
  }
}

/** Thông điệp thân thiện cho mọi loại lỗi. */
export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error && e.message) return e.message;
  return "Đã có lỗi xảy ra, vui lòng thử lại";
}
