import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { CheckCheck, MapPin, StickyNote } from "lucide-react";
import { api } from "@/api/client";
import type { AttendanceStatus, SessionAttendance } from "@/api/types";
import { Badge, Button, Card, EmptyState, ErrorState, ListSkeleton, PageHeader } from "@/components/ui";
import { ATTENDANCE_STATUS } from "@/lib/labels";
import { DAY_LONG, formatDate, formatTime, isoDayOfWeek, todayIso } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";

const ORDER: AttendanceStatus[] = ["PRESENT", "LATE", "EXCUSED", "ABSENT"];

const activeStyle: Record<AttendanceStatus, string> = {
  PRESENT: "bg-ok text-white border-ok",
  LATE: "bg-warn text-white border-warn",
  EXCUSED: "bg-info text-white border-info",
  ABSENT: "bg-danger text-white border-danger",
};

type Draft = Record<number, { status: AttendanceStatus; note: string }>;

export default function AttendanceTakePage() {
  const { sessionId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["attendance", sessionId], queryFn: () => api<SessionAttendance>(`/sessions/${sessionId}/attendance`) });
  const [draft, setDraft] = useState<Draft>({});
  const [noteOpen, setNoteOpen] = useState<number | null>(null);

  useEffect(() => {
    if (!q.data) return;
    const d: Draft = {};
    q.data.items.forEach((i) => (d[i.studentId] = { status: i.status ?? "PRESENT", note: i.note ?? "" }));
    setDraft(d);
  }, [q.data]);

  const counts = useMemo(() => {
    const c: Record<AttendanceStatus, number> = { PRESENT: 0, LATE: 0, EXCUSED: 0, ABSENT: 0 };
    Object.values(draft).forEach((v) => c[v.status]++);
    return c;
  }, [draft]);

  const save = useMutation({
    mutationFn: () => api<SessionAttendance>(`/sessions/${sessionId}/attendance`, {
      method: "PUT",
      body: { items: Object.entries(draft).map(([studentId, v]) => ({ studentId: Number(studentId), status: v.status, note: v.note || null })) },
    }),
    onSuccess: (data) => {
      qc.setQueryData(["attendance", sessionId], data);
      qc.invalidateQueries({ queryKey: ["sessions"] });
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      toast.success(`Đã lưu điểm danh lớp ${data.session.classCode}`);
      navigate(user?.role === "TEACHER" ? "/app/today" : "/app/attendance");
    },
    onError: (e) => toast.error(e.message),
  });

  if (q.isLoading) return <><PageHeader title="Điểm danh" /><Card><ListSkeleton rows={8} /></Card></>;
  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>;
  const { session, items } = q.data!;
  const future = session.date > todayIso();
  const cancelled = session.status === "CANCELLED";
  const setAll = (status: AttendanceStatus) => setDraft((d) => Object.fromEntries(Object.entries(d).map(([k, v]) => [k, { ...v, status }])));

  return (
    <div className="pb-24">
      <PageHeader
        back={{ to: user?.role === "TEACHER" ? "/app/today" : "/app/attendance", label: "Danh sách buổi học" }}
        title={session.className}
        description={
          <span className="inline-flex flex-wrap items-center gap-x-3 gap-y-1">
            <span className="tabular">{DAY_LONG[isoDayOfWeek(session.date)]}, {formatDate(session.date)} · {formatTime(session.startTime)}–{formatTime(session.endTime)}</span>
            {session.roomName && <span className="inline-flex items-center gap-1"><MapPin className="size-3.5" aria-hidden />{session.roomName}</span>}
            {q.data!.taken && <Badge tone="ok">Đã điểm danh</Badge>}
          </span>
        }
      />

      {cancelled || future ? (
        <Card><EmptyState title={cancelled ? "Buổi học đã hủy" : "Chưa đến ngày học"} description={cancelled ? session.note ?? undefined : "Điểm danh mở từ ngày diễn ra buổi học."} /></Card>
      ) : items.length === 0 ? (
        <Card><EmptyState title="Lớp chưa có học viên" description="Ghi danh học viên vào lớp trước khi điểm danh." /></Card>
      ) : (
        <>
          <div className="mb-3 flex items-center justify-between gap-2">
            <p className="text-[13px] text-ink-2">{items.length} học viên · chạm để đổi trạng thái</p>
            <Button variant="secondary" size="sm" icon={<CheckCheck className="size-4" />} onClick={() => setAll("PRESENT")}>Tất cả có mặt</Button>
          </div>
          <Card>
            <ul className="divide-y divide-line">
              {items.map((it, idx) => {
                const v = draft[it.studentId];
                if (!v) return null;
                return (
                  <li key={it.studentId} className="px-3 py-3 sm:px-5">
                    <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                      <span className="tabular w-6 text-right text-[13px] text-ink-3">{idx + 1}</span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[15px] font-medium">{it.studentName}</p>
                        <p className="text-[12px] text-ink-2">{it.studentCode}</p>
                      </div>
                      <button
                        onClick={() => setNoteOpen(noteOpen === it.studentId ? null : it.studentId)}
                        className={cn("grid size-10 place-items-center rounded-lg", v.note ? "text-primary" : "text-ink-3 hover:text-ink")}
                        aria-label={`Ghi chú cho ${it.studentName}`}
                        aria-expanded={noteOpen === it.studentId}
                      >
                        <StickyNote className="size-[18px]" />
                      </button>
                      <div role="radiogroup" aria-label={`Trạng thái của ${it.studentName}`} className="grid w-full grid-cols-4 gap-1.5 sm:w-auto">
                        {ORDER.map((st) => {
                          const on = v.status === st;
                          return (
                            <button
                              key={st}
                              role="radio"
                              aria-checked={on}
                              onClick={() => setDraft((d) => ({ ...d, [it.studentId]: { ...d[it.studentId], status: st } }))}
                              className={cn(
                                "h-12 rounded-lg border px-2 text-[13px] font-semibold transition-colors duration-[120ms] sm:min-w-[88px]",
                                on ? activeStyle[st] : "border-line-strong bg-surface text-ink-2 hover:bg-surface-2",
                              )}
                            >
                              <span className="sm:hidden">{ATTENDANCE_STATUS[st].label.replace("Vắng có phép", "Có phép")}</span>
                              <span className="hidden sm:inline">{ATTENDANCE_STATUS[st].label}</span>
                            </button>
                          );
                        })}
                      </div>
                    </div>
                    {noteOpen === it.studentId && (
                      <input
                        autoFocus
                        value={v.note}
                        maxLength={255}
                        onChange={(e) => setDraft((d) => ({ ...d, [it.studentId]: { ...d[it.studentId], note: e.target.value } }))}
                        placeholder="Ví dụ: phụ huynh xin nghỉ ốm"
                        aria-label="Ghi chú"
                        className="mt-2 h-11 w-full rounded-lg border border-line-strong px-3 text-sm focus:border-primary focus:outline-none focus:ring-[3px] focus:ring-primary/20"
                      />
                    )}
                  </li>
                );
              })}
            </ul>
          </Card>

          <div className="fixed inset-x-0 bottom-16 z-20 border-t border-line bg-surface/95 px-4 py-3 backdrop-blur-sm lg:bottom-0 lg:left-[248px]">
            <div className="mx-auto flex max-w-[1320px] items-center justify-between gap-3">
              <p className="tabular text-[13px] text-ink-2">
                <span className="font-semibold text-ok">{counts.PRESENT}</span> có mặt · <span className="font-semibold text-warn">{counts.LATE}</span> muộn ·{" "}
                <span className="font-semibold text-danger">{counts.ABSENT + counts.EXCUSED}</span> vắng
              </p>
              <Button size="lg" loading={save.isPending} onClick={() => save.mutate()}>Lưu điểm danh</Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
