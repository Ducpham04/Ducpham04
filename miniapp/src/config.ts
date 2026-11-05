export type ZaloMode = "mock" | "real";

const rawMode = (import.meta.env.VITE_ZALO_MODE ?? "auto").toLowerCase();

/** Đang chạy bên trong ứng dụng Zalo? (Zalo webview có UA chứa "Zalo"). */
export function isInsideZalo(): boolean {
  if (typeof navigator === "undefined") return false;
  return /zalo/i.test(navigator.userAgent);
}

export const config = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, ""),
  zaloMode: (rawMode === "mock" || rawMode === "real" ? rawMode : isInsideZalo() ? "real" : "mock") as ZaloMode,
  appName: "Tiện ích Phường",
  version: "1.0.0",
};
