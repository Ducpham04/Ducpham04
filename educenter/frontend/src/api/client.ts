// API client: access token trong bộ nhớ, refresh token trong localStorage, tự làm mới khi 401.

export class ApiError extends Error {
  status: number;
  errors?: Record<string, string>;
  conflicts?: Array<{ type: string; classCode: string; message?: string }>;
  constructor(status: number, message: string, body?: Record<string, unknown>) {
    super(message);
    this.status = status;
    this.errors = body?.errors as Record<string, string> | undefined;
    this.conflicts = body?.conflicts as ApiError["conflicts"];
  }
}

const REFRESH_KEY = "educenter.refresh";
const BASE = "/api/v1";

let accessToken: string | null = null;
let refreshInFlight: Promise<boolean> | null = null;
let onSessionExpired: (() => void) | null = null;

export const tokenStore = {
  setTokens(access: string, refresh: string) {
    accessToken = access;
    safeStorage.set(REFRESH_KEY, refresh);
  },
  clear() {
    accessToken = null;
    safeStorage.remove(REFRESH_KEY);
  },
  refreshToken: () => safeStorage.get(REFRESH_KEY),
  hasAccess: () => accessToken !== null,
  onExpired(cb: () => void) {
    onSessionExpired = cb;
  },
};

const safeStorage = {
  get(k: string) {
    try { return localStorage.getItem(k); } catch { return null; }
  },
  set(k: string, v: string) {
    try { localStorage.setItem(k, v); } catch { /* private mode */ }
  },
  remove(k: string) {
    try { localStorage.removeItem(k); } catch { /* ignore */ }
  },
};

type Query = Record<string, string | number | boolean | null | undefined>;

export function buildUrl(path: string, query?: Query) {
  const params = new URLSearchParams();
  Object.entries(query ?? {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") params.set(k, String(v));
  });
  const qs = params.toString();
  return `${BASE}${path}${qs ? `?${qs}` : ""}`;
}

export async function refreshSession(): Promise<boolean> {
  const refresh = tokenStore.refreshToken();
  if (!refresh) return false;
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${BASE}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: refresh }),
        });
        if (!res.ok) return false;
        const data = await res.json();
        tokenStore.setTokens(data.accessToken, data.refreshToken);
        return true;
      } catch {
        return false;
      } finally {
        setTimeout(() => (refreshInFlight = null), 0);
      }
    })();
  }
  return refreshInFlight;
}

async function raw(path: string, init: RequestInit & { query?: Query } = {}, retry = true): Promise<Response> {
  const headers = new Headers(init.headers);
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const res = await fetch(buildUrl(path, init.query), { ...init, headers });
  if (res.status === 401 && retry && !path.startsWith("/auth/login")) {
    const ok = await refreshSession();
    if (ok) return raw(path, init, false);
    tokenStore.clear();
    onSessionExpired?.();
  }
  return res;
}

async function toError(res: Response): Promise<ApiError> {
  let body: Record<string, unknown> | undefined;
  try { body = await res.json(); } catch { /* not json */ }
  const msg = (body?.detail as string) ?? (res.status >= 500 ? "Máy chủ đang gặp sự cố, vui lòng thử lại" : "Yêu cầu không thành công");
  return new ApiError(res.status, msg, body);
}

export async function api<T>(path: string, opts: { method?: string; body?: unknown; query?: Query } = {}): Promise<T> {
  let res: Response;
  try {
    res = await raw(path, {
      method: opts.method ?? "GET",
      body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
      query: opts.query,
    });
  } catch {
    throw new ApiError(0, "Không kết nối được máy chủ. Kiểm tra mạng và thử lại.");
  }
  if (!res.ok) throw await toError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

/** Tải file (PDF/Excel) có kèm token, mở hộp thoại lưu hoặc tab mới. */
export async function download(path: string, fallbackName: string, query?: Query, openInTab = false) {
  const res = await raw(path, { method: "GET", query });
  if (!res.ok) throw await toError(res);
  const blob = await res.blob();
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1]) : fallbackName;
  const url = URL.createObjectURL(blob);
  if (openInTab) {
    window.open(url, "_blank", "noopener");
  } else {
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
