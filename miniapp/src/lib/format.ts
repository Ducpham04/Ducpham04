import { config } from "../config";

const vnd = new Intl.NumberFormat("vi-VN");

export const formatVnd = (n: number) => `${vnd.format(n)} ₫`;

export function formatDate(iso?: string | null, withTime = false) {
  if (!iso) return "";
  const d = new Date(iso);
  const date = d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
  if (!withTime) return date;
  return `${d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })} ${date}`;
}

/** URL tương đối từ backend (/uploads/…, /seed/…) → URL tuyệt đối. */
export function assetUrl(url?: string | null) {
  if (!url) return undefined;
  if (/^https?:\/\//.test(url)) return url;
  return `${config.apiBaseUrl}${url.startsWith("/") ? "" : "/"}${url}`;
}

export function initials(name?: string) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return (parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0][0]).toUpperCase();
}
