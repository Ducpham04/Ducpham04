import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CalendarDays, LogOut, MapPin, Phone, UserRound } from "lucide-react";
import { api } from "@/api/client";
import type { Invoice, PortalMe, SessionView, StudentAttendance, VietQr } from "@/api/types";
import { Badge, Button, Card, CardBody, CardHeader, EmptyState, ErrorState, ListSkeleton, Skeleton, StatusChip, Tabs } from "@/components/ui";
import { QrPanel } from "@/components/domain/QrPanel";
import { ATTENDANCE_STATUS, INVOICE_STATUS, invoiceStatusKey } from "@/lib/labels";
import { DAY_LONG, formatDate, formatMoney, formatTime, isoDayOfWeek, scheduleSummary } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { ChangePasswordCard } from "../AccountPage";

type Tab = "schedule" | "attendance" | "fees" | "account";

export default function PortalPage() {
  const { logout } = useAuth();
  const [tab, setTab] = useState<Tab>("schedule");
  const me = useQuery({ queryKey: ["portal", "me"], queryFn: () => api<PortalMe>("/portal/me") });
  const m = me.data;

  return (
    <div className="mx-auto min-h-dvh max-w-2xl px-4 pb-12 pt-5">
      <header className="mb-5 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <img src="/favicon.svg" alt="" className="size-8" />
          <div><p className="text-[15px] font-bold leading-tight">{m?.centerName ?? "EduCenter"}</p><p className="text-[12px] text-ink-2">Cổng phụ huynh</p></div>
        </div>
        <Button variant="ghost" size="sm" icon={<LogOut className="size-4" />} onClick={logout}>Thoát</Button>
      </header>

      {me.isError ? <Card><ErrorState error={me.error} onRetry={() => me.refetch()} /></Card> : (
        <>
          <section className="mb-5">
            <h1 className="text-2xl font-bold tracking-[-0.02em]">{m ? m.fullName : <Skeleton className="h-8 w-48" />}</h1>
            {m && <p className="mt-0.5 text-sm text-ink-2">{m.studentCode} · {m.classes.map((c) => c.name).join(", ") || "Chưa xếp lớp"}</p>}
          </section>

          <Card className="mb-5 grid grid-cols-3 divide-x divide-line">
            <div className="px-3 py-3.5 sm:px-4"><p className="text-[12.5px] text-ink-2">Buổi tiếp theo</p>
              <p className="tabular mt-0.5 text-[15px] font-semibold">{m ? (m.nextSession ? `${formatTime(m.nextSession.startTime)} ${formatDate(m.nextSession.date).slice(0, 5)}` : "—") : <Skeleton className="h-5 w-16" />}</p></div>
            <div className="px-3 py-3.5 sm:px-4"><p className="text-[12.5px] text-ink-2">Chuyên cần</p>
              <p className="tabular mt-0.5 text-[15px] font-semibold">{m ? `${m.attendanceRate.toLocaleString("vi-VN")}%` : <Skeleton className="h-5 w-12" />}</p></div>
            <div className="px-3 py-3.5 sm:px-4"><p className="text-[12.5px] text-ink-2">Còn nợ</p>
              <p className={`tabular mt-0.5 text-[15px] font-semibold ${m && m.outstanding > 0 ? "text-danger" : ""}`}>{m ? formatMoney(m.outstanding) : <Skeleton className="h-5 w-20" />}</p></div>
          </Card>

          <Tabs<Tab> value={tab} onChange={setTab} className="mb-4" items={[
            { value: "schedule", label: "Lịch học" }, { value: "attendance", label: "Điểm danh" }, { value: "fees", label: "Học phí" }, { value: "account", label: "Tài khoản" },
          ]} />
          {tab === "schedule" && <ScheduleTab me={m} />}
          {tab === "attendance" && <AttendanceTab />}
          {tab === "fees" && <FeesTab />}
          {tab === "account" && <ChangePasswordCard />}

          {m && (
            <footer className="mt-8 space-y-1 text-[13px] text-ink-2">
              <p className="font-medium text-ink">{m.centerName}</p>
              {m.centerAddress && <p className="flex items-start gap-1.5"><MapPin className="mt-0.5 size-3.5 shrink-0" aria-hidden />{m.centerAddress}</p>}
              {m.centerPhone && <p className="flex items-center gap-1.5"><Phone className="size-3.5" aria-hidden /><a href={`tel:${m.centerPhone}`} className="tabular text-primary">{m.centerPhone}</a></p>}
            </footer>
          )}
        </>
      )}
    </div>
  );
}

function ScheduleTab({ me }: { me?: PortalMe }) {
  const q = useQuery({ queryKey: ["portal", "sessions"], queryFn: () => api<SessionView[]>("/portal/sessions") });
  return (
    <div className="space-y-4">
      {me?.classes.map((c) => (
        <Card key={c.id} className="px-4 py-3">
          <p className="font-semibold">{c.name}</p>
          <p className="tabular mt-0.5 flex items-center gap-1.5 text-[13px] text-ink-2"><CalendarDays className="size-3.5" aria-hidden />{scheduleSummary(c.schedules)}</p>
          <p className="mt-0.5 flex items-center gap-1.5 text-[13px] text-ink-2"><UserRound className="size-3.5" aria-hidden />{c.teacherName ?? "—"} · {c.roomName ?? "—"}</p>
        </Card>
      ))}
      <Card>
        <CardHeader title="30 ngày tới" />
        {q.isLoading ? <ListSkeleton rows={4} /> : !q.data?.length ? <EmptyState title="Chưa có lịch học sắp tới" /> : (
          <ul className="divide-y divide-line border-t border-line">
            {q.data.map((s) => (
              <li key={s.id} className="flex items-center gap-3 px-4 py-3">
                <div className="tabular w-24 shrink-0"><p className="text-sm font-semibold">{DAY_LONG[isoDayOfWeek(s.date)]}</p><p className="text-[12.5px] text-ink-2">{formatDate(s.date)}</p></div>
                <div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{s.className}</p><p className="tabular text-[12.5px] text-ink-2">{formatTime(s.startTime)}–{formatTime(s.endTime)} · {s.roomName}</p></div>
                {s.status === "CANCELLED" && <Badge tone="danger">Nghỉ</Badge>}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

function AttendanceTab() {
  const q = useQuery({ queryKey: ["portal", "attendance"], queryFn: () => api<StudentAttendance>("/portal/attendance") });
  if (q.isLoading) return <Card><ListSkeleton /></Card>;
  const d = q.data!;
  return (
    <Card>
      <CardHeader title={`Có mặt ${d.attended}/${d.total} buổi`} description="Tính cả đi muộn" />
      {!d.items.length ? <EmptyState title="Chưa có dữ liệu điểm danh" /> : (
        <ul className="divide-y divide-line border-t border-line">
          {d.items.slice(0, 60).map((a) => (
            <li key={a.sessionId} className="flex items-center gap-3 px-4 py-2.5">
              <span className="tabular w-20 shrink-0 text-[13px] text-ink-2">{formatDate(a.date)}</span>
              <span className="min-w-0 flex-1 truncate text-sm">{a.className}{a.note && <span className="block truncate text-[12px] text-ink-2">{a.note}</span>}</span>
              <StatusChip map={ATTENDANCE_STATUS} value={a.status} />
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function FeesTab() {
  const q = useQuery({ queryKey: ["portal", "invoices"], queryFn: () => api<Invoice[]>("/portal/invoices") });
  const [qrFor, setQrFor] = useState<number | null>(null);
  const qr = useQuery({ queryKey: ["portal", "qr", qrFor], queryFn: () => api<VietQr>(`/portal/invoices/${qrFor}/vietqr`), enabled: qrFor !== null, retry: false });
  if (q.isLoading) return <Card><ListSkeleton /></Card>;
  const open = q.data!.filter((i) => i.status === "UNPAID" || i.status === "PARTIAL");
  return (
    <div className="space-y-4">
      {open.length === 0 && <Card><CardBody className="pt-5 text-sm text-ok">Không còn khoản học phí nào cần thanh toán. Cảm ơn quý phụ huynh!</CardBody></Card>}
      {q.data!.map((i) => {
        const isOpen = i.status === "UNPAID" || i.status === "PARTIAL";
        return (
          <Card key={i.id}>
            <div className="flex items-start justify-between gap-3 px-4 py-3.5">
              <div className="min-w-0">
                <p className="font-medium">{i.description}</p>
                <p className="tabular text-[12.5px] text-ink-2">{i.code} · hạn {formatDate(i.dueDate)} · tổng {formatMoney(i.total)}</p>
              </div>
              <StatusChip map={INVOICE_STATUS} value={invoiceStatusKey(i)} />
            </div>
            {isOpen && (
              <div className="flex items-center justify-between gap-3 border-t border-line px-4 py-3">
                <p className="tabular text-sm">Còn lại <span className="text-base font-bold">{formatMoney(i.remaining)}</span></p>
                <Button size="sm" onClick={() => setQrFor(qrFor === i.id ? null : i.id)}>{qrFor === i.id ? "Ẩn mã QR" : "Chuyển khoản QR"}</Button>
              </div>
            )}
            {qrFor === i.id && (
              <div className="border-t border-line p-3">
                {qr.isLoading ? <Skeleton className="h-72" /> : qr.isError ? <p className="p-2 text-sm text-ink-2">{(qr.error as Error).message}</p> : qr.data && <QrPanel qr={qr.data} title="Quét mã để thanh toán" />}
              </div>
            )}
          </Card>
        );
      })}
    </div>
  );
}
