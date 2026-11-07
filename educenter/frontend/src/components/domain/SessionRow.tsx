import { Link } from "react-router-dom";
import { ClipboardCheck, MapPin, UserRound } from "lucide-react";
import type { SessionView } from "@/api/types";
import { formatTime } from "@/lib/format";
import { SESSION_STATUS } from "@/lib/labels";
import { StatusChip } from "@/components/ui";
import { cn } from "@/lib/cn";

/** Một dòng buổi học: giờ – lớp – phòng/GV – trạng thái – nút điểm danh. */
export function SessionRow({ s, showAction = true, showTeacher = true, compact = false }: { s: SessionView; showAction?: boolean; showTeacher?: boolean; compact?: boolean }) {
  const cancelled = s.status === "CANCELLED";
  return (
    <li className="flex items-center gap-3 px-4 py-3 sm:gap-4 sm:px-5">
      <div className="tabular w-14 shrink-0 text-center">
        <p className={cn("text-[15px] font-semibold", cancelled ? "text-ink-3 line-through" : "text-ink")}>{formatTime(s.startTime)}</p>
        <p className="text-[12px] text-ink-2">{formatTime(s.endTime)}</p>
      </div>
      <div className="min-w-0 flex-1">
        <Link to={`/app/classes/${s.classId}`} className="block truncate text-[14.5px] font-semibold text-ink hover:text-primary">{s.className}</Link>
        <p className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-[12.5px] text-ink-2">
          <span>{s.classCode}</span>
          {s.roomName && <span className="inline-flex items-center gap-1"><MapPin className="size-3.5" aria-hidden />{s.roomName}</span>}
          {showTeacher && s.teacherName && <span className="inline-flex items-center gap-1"><UserRound className="size-3.5" aria-hidden />{s.teacherName}</span>}
        </p>
      </div>
      {!compact && <div className="hidden sm:block"><StatusChip map={SESSION_STATUS} value={s.status} /></div>}
      {showAction && !cancelled && (
        <Link
          to={`/app/attendance/${s.id}`}
          className={cn(
            "inline-flex h-10 shrink-0 items-center gap-1.5 rounded-lg px-3 text-[13px] font-semibold transition-colors",
            s.status === "COMPLETED" ? "border border-line-strong text-ink hover:bg-surface-2" : "bg-primary text-white hover:bg-primary-strong",
          )}
        >
          <ClipboardCheck className="size-4" aria-hidden />
          {s.status === "COMPLETED" ? "Xem" : "Điểm danh"}
        </Link>
      )}
    </li>
  );
}
