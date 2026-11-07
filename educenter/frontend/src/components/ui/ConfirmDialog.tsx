import { createPortal } from "react-dom";
import { useEffect } from "react";
import { Button } from "./Button";

export function ConfirmDialog({ open, title, description, confirmLabel = "Xác nhận", danger, loading, onConfirm, onCancel }: {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onCancel();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onCancel]);
  if (!open) return null;
  return createPortal(
    <div className="fixed inset-0 z-50 grid place-items-center p-4">
      <div className="absolute inset-0 bg-ink/30" onClick={onCancel} aria-hidden />
      <div role="alertdialog" aria-modal="true" aria-label={title} className="relative w-full max-w-sm rounded-xl bg-surface p-5 shadow-[var(--shadow-pop)] [animation:sheet-in-up_200ms_var(--ease-out)]">
        <h2 className="text-[17px] font-semibold text-ink">{title}</h2>
        {description && <p className="mt-1.5 text-sm text-ink-2">{description}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel} autoFocus>Hủy bỏ</Button>
          <Button variant={danger ? "danger" : "primary"} loading={loading} onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
