const vnd = new Intl.NumberFormat("vi-VN");

export function formatMoney(value: number | null | undefined): string {
  return `${vnd.format(Math.round(value ?? 0))} ₫`;
}

/** Rút gọn tiền cho biểu đồ/thẻ số: 1,25 tr · 3,4 tỷ */
export function formatMoneyShort(value: number): string {
  const abs = Math.abs(value);
  if (abs >= 1e9) return `${(value / 1e9).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} tỷ`;
  if (abs >= 1e6) return `${(value / 1e6).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} tr`;
  if (abs >= 1e3) return `${(value / 1e3).toLocaleString("vi-VN", { maximumFractionDigits: 0 })} k`;
  return vnd.format(value);
}

/** "2026-11-05" → "05/11/2026" (không lệch múi giờ). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${y}`;
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

/** "18:00:00" → "18:00" */
export function formatTime(t: string | null | undefined): string {
  return t ? t.slice(0, 5) : "";
}

export const DAY_SHORT = ["", "T2", "T3", "T4", "T5", "T6", "T7", "CN"];
export const DAY_LONG = ["", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"];

/** Gom lịch tuần thành chuỗi gọn: "T2·T4·T6 18:00–19:30" */
export function scheduleSummary(slots: { dayOfWeek: number; startTime: string; endTime: string }[]): string {
  if (!slots.length) return "Chưa có lịch";
  const groups = new Map<string, number[]>();
  for (const s of slots) {
    const key = `${formatTime(s.startTime)}–${formatTime(s.endTime)}`;
    groups.set(key, [...(groups.get(key) ?? []), s.dayOfWeek]);
  }
  return [...groups.entries()]
    .map(([time, days]) => `${days.sort().map((d) => DAY_SHORT[d]).join("·")} ${time}`)
    .join(", ");
}

/** Ngày local (Asia/Ho_Chi_Minh) dạng ISO yyyy-mm-dd. */
export function toIsoDate(d: Date): string {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh", year: "numeric", month: "2-digit", day: "2-digit" }).format(d);
  return parts;
}

export function todayIso(): string {
  return toIsoDate(new Date());
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d + days));
  return date.toISOString().slice(0, 10);
}

/** Thứ (1=T2..7=CN) của ngày ISO. */
export function isoDayOfWeek(iso: string): number {
  const [y, m, d] = iso.split("-").map(Number);
  const day = new Date(Date.UTC(y, m - 1, d)).getUTCDay();
  return day === 0 ? 7 : day;
}

export function startOfWeek(iso: string): string {
  return addDays(iso, 1 - isoDayOfWeek(iso));
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const last = parts[parts.length - 1] ?? "";
  const first = parts.length > 1 ? parts[0] : "";
  return (first.charAt(0) + last.charAt(0)).toUpperCase() || "?";
}
