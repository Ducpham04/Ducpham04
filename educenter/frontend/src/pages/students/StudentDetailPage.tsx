import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { KeyRound, Mail, Pencil, Phone, Plus, Star, Trash2 } from "lucide-react";
import { api } from "@/api/client";
import type { Guardian, Invoice, Student, StudentAttendance, StudentEnrollment, StudentStatus } from "@/api/types";
import {
  Badge, Button, Card, CardBody, CardHeader, ConfirmDialog, EmptyState, ErrorState, Input, ListSkeleton, PageHeader, Select, Sheet, Skeleton, StatusChip, Tabs,
} from "@/components/ui";
import { InvoiceList } from "@/components/domain/InvoiceList";
import { ATTENDANCE_STATUS, CLASS_STATUS, ENROLLMENT_STATUS, GENDER_LABEL, STUDENT_STATUS } from "@/lib/labels";
import { formatDate, formatMoney, formatTime } from "@/lib/format";
import { StudentFormSheet, phoneSchema } from "./StudentForm";
import { InvoiceFormSheet } from "../billing/InvoiceForm";

type Tab = "info" | "classes" | "attendance" | "billing" | "portal";

export default function StudentDetailPage() {
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const tab = (params.get("tab") as Tab) ?? "info";
  const [editing, setEditing] = useState(false);
  const [invoicing, setInvoicing] = useState(false);
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["student", id], queryFn: () => api<Student>(`/students/${id}`) });

  const status = useMutation({
    mutationFn: (s: StudentStatus) => api<Student>(`/students/${id}/status`, { method: "PATCH", body: { status: s } }),
    onSuccess: (s) => {
      qc.setQueryData(["student", id], s);
      qc.invalidateQueries({ queryKey: ["students"] });
      qc.invalidateQueries({ queryKey: ["student", id, "enrollments"] });
      toast.success(`Đã chuyển trạng thái: ${STUDENT_STATUS[s.status].label}`);
    },
    onError: (e) => toast.error(e.message),
  });

  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>;
  const s = q.data;

  return (
    <>
      <PageHeader
        back={{ to: "/app/students", label: "Học viên" }}
        title={s ? s.fullName : <Skeleton className="h-8 w-56" />}
        description={s && <span className="inline-flex items-center gap-2"><span className="tabular">{s.code}</span><StatusChip map={STUDENT_STATUS} value={s.status} /></span>}
        actions={s && <>
          <Select aria-label="Đổi trạng thái" value={s.status} onChange={(e) => status.mutate(e.target.value as StudentStatus)} className="w-40" wrapperClassName="w-40">
            {Object.entries(STUDENT_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </Select>
          <Button variant="secondary" icon={<Pencil className="size-4" />} onClick={() => setEditing(true)}>Sửa hồ sơ</Button>
          <Button icon={<Plus className="size-4" />} onClick={() => setInvoicing(true)}>Lập hóa đơn</Button>
        </>}
      />
      <Tabs<Tab>
        value={tab}
        onChange={(t) => setParams({ tab: t }, { replace: true })}
        className="mb-5"
        items={[
          { value: "info", label: "Thông tin & phụ huynh" },
          { value: "classes", label: "Lịch sử lớp" },
          { value: "attendance", label: "Chuyên cần" },
          { value: "billing", label: "Học phí" },
          { value: "portal", label: "Tài khoản cổng" },
        ]}
      />
      {!s ? <Card><ListSkeleton /></Card> : (
        <>
          {tab === "info" && <InfoTab s={s} />}
          {tab === "classes" && <ClassesTab id={s.id} />}
          {tab === "attendance" && <AttendanceTab id={s.id} />}
          {tab === "billing" && <BillingTab id={s.id} onCreate={() => setInvoicing(true)} />}
          {tab === "portal" && <PortalTab s={s} />}
          <StudentFormSheet open={editing} onClose={() => setEditing(false)} student={s} />
          <InvoiceFormSheet open={invoicing} onClose={() => setInvoicing(false)} student={s} />
        </>
      )}
    </>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[120px_1fr] gap-3 py-2.5 text-sm sm:grid-cols-[160px_1fr]">
      <dt className="text-ink-2">{label}</dt>
      <dd className="min-w-0 break-words">{children || <span className="text-ink-3">—</span>}</dd>
    </div>
  );
}

function InfoTab({ s }: { s: Student }) {
  const [guardianForm, setGuardianForm] = useState<Guardian | "new" | null>(null);
  const [deleting, setDeleting] = useState<Guardian | null>(null);
  const qc = useQueryClient();
  const del = useMutation({
    mutationFn: (g: Guardian) => api(`/guardians/${g.id}`, { method: "DELETE" }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["student", String(s.id)] }); setDeleting(null); toast.success("Đã xóa phụ huynh"); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <div className="grid gap-5 lg:grid-cols-5">
      <Card className="lg:col-span-3">
        <CardHeader title="Hồ sơ" />
        <CardBody>
          <dl className="divide-y divide-line">
            <Row label="Ngày sinh">{formatDate(s.dob)}</Row>
            <Row label="Giới tính">{s.gender && GENDER_LABEL[s.gender]}</Row>
            <Row label="Điện thoại">{s.phone && <a className="tabular text-primary" href={`tel:${s.phone}`}>{s.phone}</a>}</Row>
            <Row label="Email">{s.email}</Row>
            <Row label="Trường">{s.school}</Row>
            <Row label="Trình độ đầu vào">{s.level}</Row>
            <Row label="Địa chỉ">{s.address}</Row>
            <Row label="Ngày nhập học">{formatDate(s.joinedAt)}</Row>
            <Row label="Ghi chú">{s.note}</Row>
          </dl>
        </CardBody>
      </Card>
      <Card className="lg:col-span-2">
        <CardHeader title="Phụ huynh" description="Liên hệ chính nhận email nhắc lịch & học phí" action={<Button size="sm" variant="secondary" icon={<Plus className="size-4" />} onClick={() => setGuardianForm("new")}>Thêm</Button>} />
        {s.guardians.length === 0 ? <EmptyState title="Chưa có phụ huynh" description="Học viên tự nhận nhắc nhở qua email/SĐT của mình." /> : (
          <ul className="divide-y divide-line border-t border-line">
            {s.guardians.map((g) => (
              <li key={g.id} className="flex items-start gap-3 px-4 py-3 sm:px-5">
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-2 font-medium">{g.fullName}{g.primary && <Badge tone="primary">Liên hệ chính</Badge>}</p>
                  <p className="text-[12.5px] text-ink-2">{g.relationship ?? "Phụ huynh"}</p>
                  <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-[13px]">
                    {g.phone && <a href={`tel:${g.phone}`} className="tabular inline-flex items-center gap-1 text-primary"><Phone className="size-3.5" aria-hidden />{g.phone}</a>}
                    {g.email && <span className="inline-flex items-center gap-1 text-ink-2"><Mail className="size-3.5" aria-hidden />{g.email}</span>}
                  </div>
                </div>
                <Button size="sm" variant="ghost" aria-label="Sửa phụ huynh" icon={<Pencil className="size-4" />} onClick={() => setGuardianForm(g)} />
                <Button size="sm" variant="ghost" aria-label="Xóa phụ huynh" icon={<Trash2 className="size-4" />} onClick={() => setDeleting(g)} />
              </li>
            ))}
          </ul>
        )}
      </Card>
      <GuardianSheet studentId={s.id} value={guardianForm} onClose={() => setGuardianForm(null)} />
      <ConfirmDialog open={!!deleting} title="Xóa phụ huynh?" description={deleting ? `${deleting.fullName} sẽ không còn nhận nhắc nhở.` : ""} confirmLabel="Xóa" danger loading={del.isPending}
        onConfirm={() => deleting && del.mutate(deleting)} onCancel={() => setDeleting(null)} />
    </div>
  );
}

const guardianSchema = z.object({
  fullName: z.string().trim().min(2, "Vui lòng nhập họ tên"),
  relationship: z.string().optional(),
  phone: phoneSchema.optional(),
  email: z.union([z.literal(""), z.string().email("Email không hợp lệ")]).optional(),
  zaloPhone: phoneSchema.optional(),
  primary: z.boolean(),
});

function GuardianSheet({ studentId, value, onClose }: { studentId: number; value: Guardian | "new" | null; onClose: () => void }) {
  const qc = useQueryClient();
  const g = value && value !== "new" ? value : null;
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof guardianSchema>>({
    resolver: zodResolver(guardianSchema),
    values: { fullName: g?.fullName ?? "", relationship: g?.relationship ?? "Mẹ", phone: g?.phone ?? "", email: g?.email ?? "", zaloPhone: g?.zaloPhone ?? "", primary: g?.primary ?? false },
  });
  const m = useMutation({
    mutationFn: (v: z.infer<typeof guardianSchema>) => {
      const body = { ...v, phone: v.phone || null, email: v.email || null, zaloPhone: v.zaloPhone || v.phone || null };
      return g ? api(`/guardians/${g.id}`, { method: "PUT", body }) : api(`/students/${studentId}/guardians`, { method: "POST", body });
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["student", String(studentId)] }); toast.success("Đã lưu phụ huynh"); onClose(); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Sheet open={!!value} onClose={onClose} title={g ? "Sửa phụ huynh" : "Thêm phụ huynh"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="guardian-form" loading={m.isPending}>Lưu</Button></>}>
      <form id="guardian-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
        <Input label="Họ tên" required error={errors.fullName?.message} {...register("fullName")} />
        <Select label="Quan hệ" {...register("relationship")}><option>Mẹ</option><option>Bố</option><option>Ông/Bà</option><option>Anh/Chị</option><option>Khác</option></Select>
        <Input label="Số điện thoại" inputMode="tel" error={errors.phone?.message} {...register("phone")} />
        <Input label="Số Zalo (nếu khác)" inputMode="tel" error={errors.zaloPhone?.message} {...register("zaloPhone")} />
        <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
        <label className="flex items-center gap-2.5 text-sm"><input type="checkbox" className="size-5 accent-[var(--primary)]" {...register("primary")} /><Star className="size-4 text-ink-2" aria-hidden />Đặt làm liên hệ chính</label>
      </form>
    </Sheet>
  );
}

function ClassesTab({ id }: { id: number }) {
  const q = useQuery({ queryKey: ["student", String(id), "enrollments"], queryFn: () => api<StudentEnrollment[]>(`/students/${id}/enrollments`) });
  return (
    <Card>
      <CardHeader title="Lịch sử lớp" description="Ghi danh học viên từ trang chi tiết lớp" action={<Link to="/app/classes" className="text-[13px] font-semibold text-primary">Đến danh sách lớp</Link>} />
      {q.isLoading ? <ListSkeleton rows={3} /> : !q.data?.length ? <EmptyState title="Chưa học lớp nào" /> : (
        <ul className="divide-y divide-line border-t border-line">
          {q.data.map((e) => (
            <li key={e.enrollmentId}>
              <Link to={`/app/classes/${e.classId}`} className="flex items-center gap-3 px-4 py-3 hover:bg-surface-2 sm:px-5">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{e.className}</p>
                  <p className="tabular text-[12.5px] text-ink-2">{e.classCode} · {e.courseName} · {formatDate(e.startDate)} → {e.endDate ? formatDate(e.endDate) : "nay"}</p>
                </div>
                <div className="flex flex-col items-end gap-1"><StatusChip map={ENROLLMENT_STATUS} value={e.status} /><span className="hidden sm:block"><StatusChip map={CLASS_STATUS} value={e.classStatus} /></span></div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function AttendanceTab({ id }: { id: number }) {
  const q = useQuery({ queryKey: ["student", String(id), "attendance"], queryFn: () => api<StudentAttendance>(`/students/${id}/attendance`) });
  if (q.isLoading) return <Card><ListSkeleton /></Card>;
  const d = q.data!;
  return (
    <Card>
      <CardHeader title="Chuyên cần" description={d.total ? `${d.attended}/${d.total} buổi có mặt hoặc đi muộn` : undefined}
        action={d.total > 0 && <p className="tabular text-2xl font-bold">{d.rate.toLocaleString("vi-VN")}%</p>} />
      {d.items.length === 0 ? <EmptyState title="Chưa có dữ liệu điểm danh" /> : (
        <ul className="max-h-[520px] divide-y divide-line overflow-y-auto border-t border-line">
          {d.items.map((a) => (
            <li key={a.sessionId} className="flex items-center gap-3 px-4 py-2.5 sm:px-5">
              <span className="tabular w-24 shrink-0 text-[13px] text-ink-2">{formatDate(a.date)}<br />{formatTime(a.startTime)}</span>
              <span className="min-w-0 flex-1 truncate text-sm">{a.className}{a.note && <span className="block truncate text-[12.5px] text-ink-2">{a.note}</span>}</span>
              <StatusChip map={ATTENDANCE_STATUS} value={a.status} />
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function BillingTab({ id, onCreate }: { id: number; onCreate: () => void }) {
  const q = useQuery({ queryKey: ["student", String(id), "invoices"], queryFn: () => api<Invoice[]>(`/students/${id}/invoices`) });
  const outstanding = q.data?.filter((i) => i.status === "UNPAID" || i.status === "PARTIAL").reduce((a, i) => a + i.remaining, 0) ?? 0;
  return (
    <Card>
      <CardHeader title="Học phí" description={q.data && (outstanding > 0 ? `Còn nợ ${formatMoney(outstanding)}` : "Không còn nợ")}
        action={<Button size="sm" icon={<Plus className="size-4" />} onClick={onCreate}>Lập hóa đơn</Button>} />
      {q.isLoading ? <ListSkeleton rows={3} /> : !q.data?.length ? <EmptyState title="Chưa có hóa đơn" /> : <div className="border-t border-line"><InvoiceList invoices={q.data} showStudent={false} /></div>}
    </Card>
  );
}

const portalSchema = z.object({
  email: z.string().trim().email("Email không hợp lệ"),
  password: z.string().min(8, "Mật khẩu tối thiểu 8 ký tự"),
});

function PortalTab({ s }: { s: Student }) {
  const qc = useQueryClient();
  const primary = s.guardians.find((g) => g.primary);
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof portalSchema>>({
    resolver: zodResolver(portalSchema),
    values: { email: s.portalEmail ?? primary?.email ?? s.email ?? "", password: "" },
  });
  const m = useMutation({
    mutationFn: (v: z.infer<typeof portalSchema>) => api<Student>(`/students/${s.id}/portal-account`, { method: "POST", body: v }),
    onSuccess: (st) => { qc.setQueryData(["student", String(s.id)], st); toast.success("Đã cấp tài khoản cổng phụ huynh"); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Card className="max-w-xl">
      <CardHeader title="Tài khoản cổng phụ huynh / học viên" description="Xem lịch học, điểm danh, học phí và mã QR chuyển khoản trên điện thoại." />
      <CardBody>
        {s.portalEmail && <p className="mb-4 rounded-lg bg-ok-soft px-3 py-2.5 text-sm text-ok">Đã cấp cho <strong>{s.portalEmail}</strong>. Đặt lại bên dưới nếu phụ huynh quên mật khẩu.</p>}
        <form noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
          <Input label="Email đăng nhập" type="email" error={errors.email?.message} {...register("email")} />
          <Input label={s.portalEmail ? "Mật khẩu mới" : "Mật khẩu tạm"} type="text" autoComplete="off" hint="Gửi cho phụ huynh qua Zalo; họ có thể đổi sau khi đăng nhập." error={errors.password?.message} {...register("password")} />
          <div><Button type="submit" icon={<KeyRound className="size-4" />} loading={m.isPending}>{s.portalEmail ? "Đặt lại tài khoản" : "Cấp tài khoản"}</Button></div>
        </form>
      </CardBody>
    </Card>
  );
}
