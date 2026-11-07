import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle, CheckCircle2, Plus, Trash2 } from "lucide-react";
import { api, ApiError } from "@/api/client";
import type { ClassDetail, Conflict, Slot } from "@/api/types";
import { Button, Card, CardBody, CardHeader } from "@/components/ui";
import { DAY_LONG, DAY_SHORT, formatTime } from "@/lib/format";
import { cn } from "@/lib/cn";

type EditSlot = { dayOfWeek: number; startTime: string; endTime: string };

const toEdit = (s: Slot): EditSlot => ({ dayOfWeek: s.dayOfWeek, startTime: formatTime(s.startTime), endTime: formatTime(s.endTime) });

/** Lịch học lặp theo tuần: chọn thứ + giờ, kiểm tra trùng phòng/GV trực tiếp trước khi lưu. */
export function ScheduleEditor({ cls, readOnly }: { cls: ClassDetail; readOnly: boolean }) {
  const qc = useQueryClient();
  const [slots, setSlots] = useState<EditSlot[]>(cls.schedules.map(toEdit));
  const [serverConflicts, setServerConflicts] = useState<Conflict[]>([]);
  useEffect(() => setSlots(cls.schedules.map(toEdit)), [cls.schedules]);

  const valid = slots.every((s) => s.startTime && s.endTime && s.endTime > s.startTime);
  const dirty = JSON.stringify(slots) !== JSON.stringify(cls.schedules.map(toEdit));
  const [debounced, setDebounced] = useState(slots);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(slots), 350);
    return () => clearTimeout(t);
  }, [slots]);

  const check = useQuery({
    queryKey: ["conflicts", cls.id, cls.roomId, cls.teacherId, cls.startDate, cls.endDate, debounced],
    queryFn: () => api<Conflict[]>("/schedule/check-conflicts", {
      method: "POST",
      body: { classId: cls.id, roomId: cls.roomId, teacherId: cls.teacherId, startDate: cls.startDate, endDate: cls.endDate, slots: debounced },
    }),
    enabled: !readOnly && dirty && valid && debounced.length > 0,
  });
  const conflicts = dirty ? check.data ?? [] : [];
  const shown = conflicts.length ? conflicts : serverConflicts;

  const save = useMutation({
    mutationFn: () => api<ClassDetail>(`/classes/${cls.id}/schedules`, { method: "PUT", body: { slots } }),
    onSuccess: (c) => {
      setServerConflicts([]);
      qc.setQueryData(["class", String(cls.id)], c);
      qc.invalidateQueries({ queryKey: ["class", String(cls.id), "sessions"] });
      qc.invalidateQueries({ queryKey: ["sessions"] });
      qc.invalidateQueries({ queryKey: ["classes"] });
      toast.success(`Đã lưu lịch và sinh ${c.totalSessions - c.completedSessions} buổi học sắp tới`);
    },
    onError: (e) => {
      if (e instanceof ApiError && e.conflicts) setServerConflicts(e.conflicts as Conflict[]);
      toast.error(e.message, { duration: 8000 });
    },
  });

  const update = (i: number, patch: Partial<EditSlot>) => setSlots((s) => s.map((x, j) => (i === j ? { ...x, ...patch } : x)));
  const conflictFor = (s: EditSlot) => shown.filter((c) => c.dayOfWeek === s.dayOfWeek && c.startTime.slice(0, 5) < s.endTime && c.endTime.slice(0, 5) > s.startTime);

  return (
    <Card>
      <CardHeader title="Lịch học hằng tuần" description={readOnly ? undefined : "Lưu lịch sẽ sinh lại các buổi học chưa điểm danh từ hôm nay đến ngày kết thúc lớp."} />
      <CardBody className="space-y-3">
        {!cls.roomId || !cls.teacherId ? (
          <p className="rounded-lg bg-warn-soft px-3 py-2.5 text-sm text-warn">Lớp chưa có {!cls.teacherId ? "giáo viên" : "phòng"} – hệ thống chỉ kiểm tra trùng được khi đã phân công.</p>
        ) : null}
        {slots.length === 0 && <p className="py-4 text-center text-sm text-ink-2">Chưa có khung giờ nào.</p>}
        {slots.map((s, i) => {
          const cs = conflictFor(s);
          return (
            <div key={i} className={cn("rounded-lg border p-3", cs.length ? "border-danger bg-danger-soft/40" : "border-line")}>
              <div className="flex flex-wrap items-end gap-3">
                <div className="w-full sm:w-auto">
                  <p className="mb-1.5 text-[13px] font-medium">Thứ</p>
                  <div role="radiogroup" aria-label="Chọn thứ" className="grid grid-cols-7 gap-1">
                    {[1, 2, 3, 4, 5, 6, 7].map((d) => (
                      <button key={d} type="button" role="radio" aria-checked={s.dayOfWeek === d} aria-label={DAY_LONG[d]} disabled={readOnly}
                        onClick={() => update(i, { dayOfWeek: d })}
                        className={cn("h-10 min-w-10 rounded-lg border text-[13px] font-semibold transition-colors",
                          s.dayOfWeek === d ? "border-primary bg-primary text-white" : "border-line-strong text-ink-2 hover:bg-surface-2")}>
                        {DAY_SHORT[d]}
                      </button>
                    ))}
                  </div>
                </div>
                <label className="flex flex-col gap-1.5 text-[13px] font-medium">Bắt đầu
                  <input type="time" value={s.startTime} disabled={readOnly} onChange={(e) => update(i, { startTime: e.target.value })} className="h-10 rounded-lg border border-line-strong px-2 font-normal" />
                </label>
                <label className="flex flex-col gap-1.5 text-[13px] font-medium">Kết thúc
                  <input type="time" value={s.endTime} disabled={readOnly} onChange={(e) => update(i, { endTime: e.target.value })} className="h-10 rounded-lg border border-line-strong px-2 font-normal" />
                </label>
                {!readOnly && <Button variant="ghost" size="sm" aria-label="Xóa khung giờ" icon={<Trash2 className="size-4" />} onClick={() => setSlots((x) => x.filter((_, j) => j !== i))} />}
              </div>
              {s.endTime && s.startTime && s.endTime <= s.startTime && <p className="mt-2 text-[13px] text-danger">Giờ kết thúc phải sau giờ bắt đầu.</p>}
              {cs.map((c, k) => (
                <p key={k} className="mt-2 flex items-start gap-1.5 text-[13px] text-danger"><AlertTriangle className="mt-0.5 size-3.5 shrink-0" aria-hidden />{c.message}</p>
              ))}
            </div>
          );
        })}
        {!readOnly && (
          <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
            <Button variant="secondary" size="sm" icon={<Plus className="size-4" />} onClick={() => setSlots((s) => [...s, { dayOfWeek: s.length ? Math.min(7, s[s.length - 1].dayOfWeek + 2) : 2, startTime: s[s.length - 1]?.startTime ?? "18:00", endTime: s[s.length - 1]?.endTime ?? "19:30" }])}>
              Thêm khung giờ
            </Button>
            <div className="flex items-center gap-3">
              {dirty && valid && slots.length > 0 && !check.isFetching && check.data && conflicts.length === 0 && (
                <span className="inline-flex items-center gap-1 text-[13px] text-ok"><CheckCircle2 className="size-4" aria-hidden />Không trùng lịch</span>
              )}
              <Button disabled={!dirty || !valid || conflicts.length > 0} loading={save.isPending} onClick={() => save.mutate()}>Lưu lịch học</Button>
            </div>
          </div>
        )}
      </CardBody>
    </Card>
  );
}
