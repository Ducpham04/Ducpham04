import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { api } from "@/api/client";
import { useRooms, useTeachers } from "@/api/hooks";
import type { SessionView } from "@/api/types";
import { Button, Card, EmptyState, ErrorState, PageHeader, Select, Skeleton, Tabs } from "@/components/ui";
import { SessionRow } from "@/components/domain/SessionRow";
import { addDays, DAY_LONG, DAY_SHORT, formatDate, formatTime, isoDayOfWeek, startOfWeek, todayIso } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";

type Mode = "week" | "month";

/** Màu phân biệt lớp: dùng độ sáng/độ đậm khác nhau của primary + viền, không dùng bảng màu rực. */
const CLASS_TINTS = [
  "bg-primary-soft text-primary-strong",
  "bg-ok-soft text-ok",
  "bg-warn-soft text-warn",
  "bg-info-soft text-info",
  "bg-danger-soft text-danger",
  "bg-surface-2 text-ink",
];
const tint = (classId: number) => CLASS_TINTS[classId % CLASS_TINTS.length];

function monthRange(iso: string) {
  const first = iso.slice(0, 8) + "01";
  const [y, m] = first.split("-").map(Number);
  const last = new Date(Date.UTC(y, m, 0)).toISOString().slice(0, 10);
  const gridStart = startOfWeek(first);
  const gridEnd = addDays(startOfWeek(last), 6);
  return { first, last, gridStart, gridEnd };
}

export default function SchedulePage() {
  const { hasRole } = useAuth();
  const isManager = hasRole("CENTER_ADMIN", "STAFF");
  const [mode, setMode] = useState<Mode>("week");
  const [anchor, setAnchor] = useState(todayIso());
  const [teacherId, setTeacherId] = useState("");
  const [roomId, setRoomId] = useState("");
  const teachers = useTeachers();
  const rooms = useRooms();
  const today = todayIso();

  const range = useMemo(() => {
    if (mode === "week") {
      const from = startOfWeek(anchor);
      return { from, to: addDays(from, 6) };
    }
    const r = monthRange(anchor);
    return { from: r.gridStart, to: r.gridEnd };
  }, [mode, anchor]);

  const q = useQuery({
    queryKey: ["sessions", range.from, range.to, teacherId, roomId],
    queryFn: () => api<SessionView[]>("/sessions", { query: { from: range.from, to: range.to, teacherId, roomId } }),
  });
  const byDate = useMemo(() => {
    const m = new Map<string, SessionView[]>();
    q.data?.forEach((s) => m.set(s.date, [...(m.get(s.date) ?? []), s]));
    return m;
  }, [q.data]);

  const shift = (dir: number) => {
    if (mode === "week") setAnchor(addDays(anchor, dir * 7));
    else {
      const [y, m] = anchor.split("-").map(Number);
      setAnchor(new Date(Date.UTC(y, m - 1 + dir, 1)).toISOString().slice(0, 10));
    }
  };
  const title = mode === "week"
    ? `${formatDate(range.from).slice(0, 5)} – ${formatDate(range.to)}`
    : `Tháng ${Number(anchor.slice(5, 7))}/${anchor.slice(0, 4)}`;
  const days = Array.from({ length: 7 }, (_, i) => addDays(range.from, i));

  return (
    <>
      <PageHeader title={isManager ? "Lịch học" : "Lịch dạy"} description={isManager ? "Toàn bộ buổi học theo tuần / tháng" : "Các buổi bạn phụ trách"} />
      <Card className="mb-4 flex flex-wrap items-center gap-3 px-3 py-3 sm:px-4">
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" aria-label="Kỳ trước" icon={<ChevronLeft className="size-4" />} onClick={() => shift(-1)} />
          <p className="tabular min-w-[150px] text-center text-[15px] font-semibold">{title}</p>
          <Button variant="ghost" size="sm" aria-label="Kỳ sau" icon={<ChevronRight className="size-4" />} onClick={() => shift(1)} />
          <Button variant="secondary" size="sm" onClick={() => setAnchor(today)}>Hôm nay</Button>
        </div>
        <Tabs<Mode> value={mode} onChange={setMode} items={[{ value: "week", label: "Tuần" }, { value: "month", label: "Tháng" }]} className="mx-0 border-b-0 px-0" />
        {isManager && (
          <div className="flex w-full gap-2 sm:ml-auto sm:w-auto">
            <Select aria-label="Lọc giáo viên" value={teacherId} onChange={(e) => setTeacherId(e.target.value)} wrapperClassName="flex-1 sm:w-44">
              <option value="">Tất cả giáo viên</option>
              {teachers.data?.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
            </Select>
            <Select aria-label="Lọc phòng" value={roomId} onChange={(e) => setRoomId(e.target.value)} wrapperClassName="flex-1 sm:w-36">
              <option value="">Tất cả phòng</option>
              {rooms.data?.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </Select>
          </div>
        )}
      </Card>

      {q.isError ? <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card> : q.isLoading ? <Skeleton className="h-96 w-full" /> : mode === "week" ? (
        <>
          {/* Desktop: 7 cột */}
          <div className="hidden grid-cols-7 gap-2 lg:grid">
            {days.map((d) => (
              <div key={d} className={cn("min-h-[420px] rounded-xl border bg-surface", d === today ? "border-primary" : "border-line")}>
                <div className={cn("border-b px-3 py-2", d === today ? "border-primary/30 bg-primary-soft" : "border-line")}>
                  <p className={cn("text-[13px] font-semibold", d === today && "text-primary-strong")}>{DAY_LONG[isoDayOfWeek(d)]}</p>
                  <p className="tabular text-[12px] text-ink-2">{formatDate(d).slice(0, 5)}</p>
                </div>
                <div className="space-y-1.5 p-1.5">
                  {(byDate.get(d) ?? []).map((s) => (
                    <Link key={s.id} to={`/app/classes/${s.classId}?tab=sessions`}
                      className={cn("block rounded-lg px-2 py-1.5 text-[12.5px] leading-snug transition-[filter] hover:brightness-95", tint(s.classId), s.status === "CANCELLED" && "line-through opacity-60")}>
                      <span className="tabular block font-semibold">{formatTime(s.startTime)}–{formatTime(s.endTime)}</span>
                      <span className="block truncate font-medium">{s.classCode}</span>
                      <span className="block truncate opacity-80">{s.roomName}{isManager && s.teacherName && ` · ${s.teacherName.split(" ").pop()}`}</span>
                    </Link>
                  ))}
                </div>
              </div>
            ))}
          </div>
          {/* Mobile: danh sách theo ngày */}
          <div className="space-y-4 lg:hidden">
            {days.map((d) => (
              <section key={d}>
                <h2 className={cn("mb-1.5 text-[13px] font-semibold", d === today ? "text-primary" : "text-ink-2")}>{DAY_LONG[isoDayOfWeek(d)]}, {formatDate(d)}{d === today && " · Hôm nay"}</h2>
                <Card>
                  {(byDate.get(d) ?? []).length ? (
                    <ul className="divide-y divide-line">{byDate.get(d)!.map((s) => <SessionRow key={s.id} s={s} showTeacher={isManager} showAction={s.date <= today} />)}</ul>
                  ) : <p className="px-4 py-3 text-sm text-ink-3">Không có buổi học</p>}
                </Card>
              </section>
            ))}
          </div>
          {q.data?.length === 0 && <EmptyState title="Tuần này không có buổi học" className="lg:hidden" />}
        </>
      ) : (
        <Card className="overflow-hidden">
          <div className="grid grid-cols-7 border-b border-line bg-surface-2 text-center text-[12.5px] font-medium text-ink-2">
            {[1, 2, 3, 4, 5, 6, 7].map((d) => <div key={d} className="py-2">{DAY_SHORT[d]}</div>)}
          </div>
          <div className="grid grid-cols-7">
            {Array.from({ length: Math.round((Date.parse(range.to) - Date.parse(range.from)) / 864e5) + 1 }, (_, i) => addDays(range.from, i)).map((d) => {
              const inMonth = d.slice(0, 7) === anchor.slice(0, 7);
              const list = byDate.get(d) ?? [];
              return (
                <button key={d} type="button" onClick={() => { setAnchor(d); setMode("week"); }}
                  className={cn("min-h-[72px] border-b border-r border-line p-1 text-left align-top hover:bg-surface-2 sm:min-h-[104px] sm:p-1.5", !inMonth && "bg-surface-2/50 text-ink-3")}>
                  <span className={cn("tabular inline-grid size-6 place-items-center rounded-full text-[12.5px]", d === today && "bg-primary font-semibold text-white")}>{Number(d.slice(8))}</span>
                  <span className="mt-0.5 hidden space-y-0.5 sm:block">
                    {list.slice(0, 3).map((s) => (
                      <span key={s.id} className={cn("block truncate rounded px-1 text-[11.5px]", tint(s.classId), s.status === "CANCELLED" && "line-through opacity-60")}>
                        {formatTime(s.startTime)} {s.classCode}
                      </span>
                    ))}
                    {list.length > 3 && <span className="block px-1 text-[11.5px] text-ink-2">+{list.length - 3} buổi</span>}
                  </span>
                  {list.length > 0 && <span className="tabular mt-1 block text-[11px] font-medium text-primary sm:hidden">{list.length} buổi</span>}
                </button>
              );
            })}
          </div>
        </Card>
      )}
    </>
  );
}
