import type { FeedbackStatus, OrderStatus } from "../api/types";

const MAP: Record<FeedbackStatus | OrderStatus, { label: string; cls: string }> = {
  NEW: { label: "Mới tiếp nhận", cls: "bg-primary-soft text-primary" },
  IN_PROGRESS: { label: "Đang xử lý", cls: "bg-amber-50 text-warning" },
  RESOLVED: { label: "Đã xử lý", cls: "bg-green-50 text-success" },
  REJECTED: { label: "Từ chối", cls: "bg-red-50 text-danger" },
  PENDING: { label: "Chờ thanh toán", cls: "bg-amber-50 text-warning" },
  PAID: { label: "Đã thanh toán", cls: "bg-green-50 text-success" },
  FAILED: { label: "Thất bại", cls: "bg-red-50 text-danger" },
  CANCELLED: { label: "Đã huỷ", cls: "bg-gray-100 text-ink-muted" },
};

export function StatusBadge({ status }: { status: FeedbackStatus | OrderStatus }) {
  const m = MAP[status];
  return <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${m.cls}`}>{m.label}</span>;
}

export const statusLabel = (s: FeedbackStatus | OrderStatus) => MAP[s].label;
