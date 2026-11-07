import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";

export function Pagination({ page, totalPages, totalItems, onChange }: { page: number; totalPages: number; totalItems: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return <p className="px-4 py-3 text-[13px] text-ink-2 sm:px-5">{totalItems} kết quả</p>;
  return (
    <div className="flex items-center justify-between gap-3 border-t border-line px-4 py-3 sm:px-5">
      <p className="tabular text-[13px] text-ink-2">Trang {page + 1}/{totalPages} · {totalItems} kết quả</p>
      <div className="flex gap-1">
        <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label="Trang trước" icon={<ChevronLeft className="size-4" />} />
        <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)} aria-label="Trang sau" icon={<ChevronRight className="size-4" />} />
      </div>
    </div>
  );
}
