import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { Pencil, UserMinus, UserPlus } from "lucide-react";
import { api } from "@/api/client";
import type { ClassDetail, ClassStudent, Page, SessionView, StudentListItem } from "@/api/types";
import { Button, Card, CardHeader, ConfirmDialog, EmptyState, ErrorState, ListSkeleton, PageHeader, Select, Sheet, Skeleton, StatusChip, Tabs, SearchInput } from "@/components/ui";
import { CLASS_STATUS, ENROLLMENT_STATUS, SESSION_STATUS, STUDENT_STATUS } from "@/lib/labels";
import { DAY_SHORT, formatDate, formatTime, isoDayOfWeek, scheduleSummary, todayIso } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { ClassFormSheet } from "./ClassForm";
import { ScheduleEditor } from "./ScheduleEditor";

type Tab = "students" | "schedule" | "sessions";

export default function ClassDetailPage() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const canEdit = hasRole("CENTER_ADMIN", "STAFF");
  const [params, setParams] = useSearchParams();
  const tab = (params.get("tab") as Tab) ?? "students";
  const [editing, setEditing] = useState(false);
  const q = useQuery({ queryKey: ["class", id], queryFn: () => api<ClassDetail>(`/classes/${id}`) });
  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>;
  const c = q.data;

  return (
    <>
      <PageHeader
        back={{ to: "/app/classes", label: "Lớp học" }}
        title={c ? c.name : <Skeleton className="h-8 w-64" />}
        description={c && <span className="inline-flex flex-wrap items-center gap-2"><span>{c.code} · {c.courseName}</span><StatusChip map={CLASS_STATUS} value={c.status} /></span>}
        actions={c && canEdit && <Button variant="secondary" icon={<Pencil className="size-4" />} onClick={() => setEditing(true)}>Sửa lớp</Button>}
      />
      {c && (
        <Card className="mb-5 grid grid-cols-2 gap-y-3 px-4 py-4 text-sm sm:grid-cols-4 sm:px-5">
          <Info label="Giáo viên" value={c.teacherName ?? "Chưa phân công"} />
          <Info label="Phòng" value={c.roomName ?? "Chưa xếp phòng"} />
          <Info label="Sĩ số" value={`${c.enrolledCount}/${c.maxStudents} học viên`} />
          <Info label="Tiến độ" value={`${c.completedSessions}/${c.totalSessions} buổi`} />
          <Info label="Lịch" value={scheduleSummary(c.schedules)} className="col-span-2" />
          <Info label="Thời gian" value={`${formatDate(c.startDate)} → ${formatDate(c.endDate)}`} className="col-span-2" />
        </Card>
      )}
      <Tabs<Tab> value={tab} onChange={(t) => setParams({ tab: t }, { replace: true })} className="mb-5"
        items={[{ value: "students", label: "Học viên", count: c?.enrolledCount }, { value: "schedule", label: "Lịch tuần" }, { value: "sessions", label: "Buổi học" }]} />
      {!c ? <Card><ListSkeleton /></Card> : (
        <>
          {tab === "students" && <StudentsTab cls={c} canEdit={canEdit} />}
          {tab === "schedule" && <ScheduleEditor cls={c} readOnly={!canEdit} />}
          {tab === "sessions" && <SessionsTab cls={c} />}
          {canEdit && <ClassFormSheet open={editing} onClose={() => setEditing(false)} value={c} />}
        </>
      )}
    </>
  );
}

function Info({ label, value, className }: { label: string; value: string; className?: string }) {
  return <div className={className}><p className="text-[12.5px] text-ink-2">{label}</p><p className="tabular font-medium">{value}</p></div>;
}

function StudentsTab({ cls, canEdit }: { cls: ClassDetail; canEdit: boolean }) {
  const qc = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<ClassStudent | null>(null);
  const q = useQuery({ queryKey: ["class", String(cls.id), "students"], queryFn: () => api<ClassStudent[]>(`/classes/${cls.id}/students`) });
  const end = useMutation({
    mutationFn: (e: ClassStudent) => api(`/enrollments/${e.enrollmentId}/end`, { method: "POST", body: { status: "DROPPED" } }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["class", String(cls.id)] });
      qc.invalidateQueries({ queryKey: ["classes"] });
      setRemoving(null);
      toast.success("Đã chuyển học viên khỏi lớp");
    },
    onError: (e) => toast.error(e.message),
  });
  const active = q.data?.filter((e) => e.status === "ACTIVE") ?? [];
  const past = q.data?.filter((e) => e.status !== "ACTIVE") ?? [];
  const full = cls.enrolledCount >= cls.maxStudents;

  return (
    <Card>
      <CardHeader title="Học viên trong lớp" description={full ? "Lớp đã đủ sĩ số" : `Còn ${cls.maxStudents - cls.enrolledCount} chỗ`}
        action={canEdit && <Button size="sm" icon={<UserPlus className="size-4" />} disabled={full} onClick={() => setAdding(true)}>Ghi danh</Button>} />
      {q.isLoading ? <ListSkeleton /> : !q.data?.length ? <EmptyState title="Lớp chưa có học viên" description={canEdit ? "Bấm Ghi danh để thêm học viên vào lớp." : undefined} /> : (
        <ul className="divide-y divide-line border-t border-line">
          {[...active, ...past].map((e) => (
            <li key={e.enrollmentId} className="flex items-center gap-3 px-4 py-3 sm:px-5">
              <div className="min-w-0 flex-1">
                {canEdit ? <Link to={`/app/students/${e.studentId}`} className="truncate font-medium hover:text-primary">{e.studentName}</Link> : <p className="truncate font-medium">{e.studentName}</p>}
                <p className="tabular text-[12.5px] text-ink-2">{e.studentCode} · vào lớp {formatDate(e.startDate)}{e.endDate && ` · rời ${formatDate(e.endDate)}`}</p>
              </div>
              <div className="tabular hidden w-24 text-right sm:block">
                <p className="text-sm font-semibold">{e.attendanceRate === null ? "—" : `${e.attendanceRate.toLocaleString("vi-VN")}%`}</p>
                <p className="text-[12px] text-ink-2">chuyên cần</p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <StatusChip map={ENROLLMENT_STATUS} value={e.status} />
                {e.status === "ACTIVE" && e.studentStatus !== "ACTIVE" && <StatusChip map={STUDENT_STATUS} value={e.studentStatus} />}
              </div>
              {canEdit && e.status === "ACTIVE" && <Button size="sm" variant="ghost" aria-label={`Chuyển ${e.studentName} khỏi lớp`} icon={<UserMinus className="size-4" />} onClick={() => setRemoving(e)} />}
            </li>
          ))}
        </ul>
      )}
      {canEdit && <EnrollSheet cls={cls} open={adding} onClose={() => setAdding(false)} existing={active.map((a) => a.studentId)} />}
      <ConfirmDialog open={!!removing} title="Chuyển học viên khỏi lớp?" description={removing ? `${removing.studentName} sẽ không còn trong danh sách điểm danh. Lịch sử học vẫn được giữ.` : ""}
        confirmLabel="Chuyển khỏi lớp" danger loading={end.isPending} onConfirm={() => removing && end.mutate(removing)} onCancel={() => setRemoving(null)} />
    </Card>
  );
}

function EnrollSheet({ cls, open, onClose, existing }: { cls: ClassDetail; open: boolean; onClose: () => void; existing: number[] }) {
  const qc = useQueryClient();
  const [q, setQ] = useState("");
  const list = useQuery({ queryKey: ["students", "enroll", q], queryFn: () => api<Page<StudentListItem>>("/students", { query: { q, size: 30 } }), enabled: open });
  const enroll = useMutation({
    mutationFn: (studentId: number) => api<ClassStudent>(`/classes/${cls.id}/enrollments`, { method: "POST", body: { studentId } }),
    onSuccess: (e) => {
      qc.invalidateQueries({ queryKey: ["class", String(cls.id)] });
      qc.invalidateQueries({ queryKey: ["classes"] });
      toast.success(`Đã ghi danh ${e.studentName}`);
    },
    onError: (e) => toast.error(e.message),
  });
  const candidates = list.data?.items.filter((s) => !existing.includes(s.id) && s.status !== "DROPPED") ?? [];
  return (
    <Sheet open={open} onClose={onClose} title={`Ghi danh vào ${cls.code}`} description={`${cls.enrolledCount}/${cls.maxStudents} học viên`}>
      <SearchInput value={q} onChange={setQ} placeholder="Tìm học viên theo tên, SĐT" />
      <ul className="mt-3 divide-y divide-line">
        {list.isLoading ? <ListSkeleton rows={4} /> : candidates.length === 0 ? <EmptyState title="Không có học viên phù hợp" /> : candidates.map((s) => (
          <li key={s.id} className="flex items-center gap-3 py-2.5">
            <div className="min-w-0 flex-1"><p className="truncate font-medium">{s.fullName}</p><p className="tabular text-[12.5px] text-ink-2">{s.code} · {s.phone ?? s.guardianPhone ?? ""}</p></div>
            <StatusChip map={STUDENT_STATUS} value={s.status} />
            <Button size="sm" variant="secondary" loading={enroll.isPending && enroll.variables === s.id} onClick={() => enroll.mutate(s.id)}>Thêm</Button>
          </li>
        ))}
      </ul>
    </Sheet>
  );
}

function SessionsTab({ cls }: { cls: ClassDetail }) {
  const { hasRole } = useAuth();
  const canEdit = hasRole("CENTER_ADMIN", "STAFF");
  const qc = useQueryClient();
  const [filter, setFilter] = useState<"upcoming" | "past">("upcoming");
  const [cancelling, setCancelling] = useState<SessionView | null>(null);
  const [reason, setReason] = useState("Nghỉ lễ");
  const q = useQuery({ queryKey: ["class", String(cls.id), "sessions"], queryFn: () => api<SessionView[]>(`/classes/${cls.id}/sessions`) });
  const cancel = useMutation({
    mutationFn: (s: SessionView) => api(`/sessions/${s.id}`, { method: "PATCH", body: { status: "CANCELLED", note: reason } }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["class", String(cls.id)] }); qc.invalidateQueries({ queryKey: ["sessions"] }); setCancelling(null); toast.success("Đã hủy buổi học"); },
    onError: (e) => toast.error(e.message),
  });
  const today = todayIso();
  const list = (q.data ?? []).filter((s) => (filter === "upcoming" ? s.date >= today : s.date < today));
  const ordered = filter === "past" ? [...list].reverse() : list;
  return (
    <Card>
      <div className="flex items-center justify-between gap-3 px-4 pt-3 sm:px-5">
        <Tabs value={filter} onChange={setFilter} items={[{ value: "upcoming", label: "Sắp tới" }, { value: "past", label: "Đã qua" }]} className="border-b-0" />
      </div>
      {q.isLoading ? <ListSkeleton /> : ordered.length === 0 ? <EmptyState title={filter === "upcoming" ? "Chưa có buổi học sắp tới" : "Chưa có buổi học nào"} description={filter === "upcoming" ? "Đặt lịch tuần để hệ thống sinh buổi học." : undefined} /> : (
        <ul className="max-h-[560px] divide-y divide-line overflow-y-auto border-t border-line">
          {ordered.map((s) => (
            <li key={s.id} className="flex items-center gap-3 px-4 py-2.5 sm:px-5">
              <div className="tabular w-28 shrink-0 text-sm"><p className="font-medium">{DAY_SHORT[isoDayOfWeek(s.date)]}, {formatDate(s.date).slice(0, 5)}</p><p className="text-[12.5px] text-ink-2">{formatTime(s.startTime)}–{formatTime(s.endTime)}</p></div>
              <p className="min-w-0 flex-1 truncate text-[13px] text-ink-2">{s.roomName}{s.note && ` · ${s.note}`}{s.status === "COMPLETED" && ` · ${s.attendanceCount} lượt điểm danh`}</p>
              <StatusChip map={SESSION_STATUS} value={s.status} />
              {s.status !== "CANCELLED" && s.date <= today && <Link to={`/app/attendance/${s.id}`} className="text-[13px] font-semibold text-primary">Điểm danh</Link>}
              {canEdit && s.status === "SCHEDULED" && s.date >= today && <Button size="sm" variant="ghost" onClick={() => setCancelling(s)}>Hủy</Button>}
            </li>
          ))}
        </ul>
      )}
      <Sheet open={!!cancelling} onClose={() => setCancelling(null)} title="Hủy buổi học" description={cancelling ? `${formatDate(cancelling.date)} · ${formatTime(cancelling.startTime)}` : ""}
        footer={<><Button variant="secondary" onClick={() => setCancelling(null)}>Đóng</Button><Button variant="danger" loading={cancel.isPending} onClick={() => cancelling && cancel.mutate(cancelling)}>Hủy buổi học</Button></>}>
        <Select label="Lý do" value={reason} onChange={(e) => setReason(e.target.value)}>
          <option>Nghỉ lễ</option><option>Giáo viên bận – sẽ học bù</option><option>Thời tiết xấu</option><option>Khác</option>
        </Select>
      </Sheet>
    </Card>
  );
}
