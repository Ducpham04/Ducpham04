import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "@/api/client";
import type { Dashboard } from "@/api/types";
import { Card, CardBody, CardHeader, EmptyState, ErrorState, PageHeader, Skeleton } from "@/components/ui";
import { SessionRow } from "@/components/domain/SessionRow";
import { formatDate, formatMoney, formatMoneyShort } from "@/lib/format";
import { useAuth } from "@/lib/auth";

function Delta({ now, prev, unit = "" }: { now: number; prev: number; unit?: string }) {
  if (!prev) return <span className="text-ink-3">—</span>;
  const pct = Math.round(((now - prev) / prev) * 100);
  const up = pct >= 0;
  return (
    <span className={up ? "text-ok" : "text-danger"}>
      {up ? "▲" : "▼"} {Math.abs(pct)}%{unit} so với tháng trước
    </span>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const q = useQuery({ queryKey: ["dashboard"], queryFn: () => api<Dashboard>("/reports/dashboard") });
  const d = q.data;
  const hour = Number(new Intl.DateTimeFormat("en-GB", { hour: "numeric", hour12: false, timeZone: "Asia/Ho_Chi_Minh" }).format(new Date()));
  const greet = hour < 11 ? "Chào buổi sáng" : hour < 18 ? "Chào buổi chiều" : "Chào buổi tối";

  return (
    <>
      <PageHeader title={`${greet}, ${user?.fullName.split(" ").pop()}`} description={user?.tenantName ?? undefined} />
      {q.isError ? (
        <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>
      ) : (
        <div className="space-y-5">
          {/* Dải số liệu: 1 card chia cột bằng divider */}
          <Card className="grid grid-cols-2 divide-line lg:grid-cols-4 lg:divide-x [&>*]:border-line max-lg:[&>*:nth-child(-n+2)]:border-b max-lg:[&>*:nth-child(odd)]:border-r">
            <Stat label="Doanh thu tháng này" value={d && formatMoney(d.revenueThisMonth)} sub={d && <Delta now={d.revenueThisMonth} prev={d.revenueLastMonth} />} />
            <Stat label="Công nợ hiện tại" value={d && formatMoney(d.outstanding)} sub={d && (d.overdue > 0 ? <span className="text-danger">{formatMoney(d.overdue)} quá hạn</span> : "Không có nợ quá hạn")} />
            <Stat label="Học viên mới tháng này" value={d && String(d.newStudentsThisMonth)} sub={d && `${d.activeStudents} học viên đang học`} />
            <Stat label="Chuyên cần 30 ngày" value={d && `${d.attendanceRate30d.toLocaleString("vi-VN")}%`} sub={d && `${d.activeClasses} lớp đang hoạt động`} />
          </Card>

          <div className="grid gap-5 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader title="Doanh thu 12 tháng" description="Tổng tiền đã thu theo tháng" action={<Link to="/app/reports" className="text-[13px] font-semibold text-primary">Xem báo cáo</Link>} />
              <CardBody>
                {d ? (
                  <div className="h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={d.revenue12Months.map((m) => ({ name: `T${m.month % 100}`, value: Number(m.value) }))} margin={{ left: -8, right: 4, top: 8 }}>
                        <CartesianGrid vertical={false} stroke="var(--line)" />
                        <XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fill: "var(--ink-2)", fontSize: 12 }} />
                        <YAxis tickLine={false} axisLine={false} tickFormatter={formatMoneyShort} tick={{ fill: "var(--ink-2)", fontSize: 12 }} width={56} />
                        <Tooltip cursor={{ fill: "var(--surface-2)" }} formatter={(v) => [formatMoney(Number(v)), "Doanh thu"]} contentStyle={{ borderRadius: 10, border: "1px solid var(--line)", fontSize: 13 }} />
                        <Bar dataKey="value" fill="var(--primary)" radius={[4, 4, 0, 0]} maxBarSize={36} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                ) : <Skeleton className="h-64 w-full" />}
              </CardBody>
            </Card>

            <Card>
              <CardHeader title="Buổi học hôm nay" description={d ? `${d.sessionsToday.length} buổi` : undefined} action={<Link to="/app/schedule" className="text-[13px] font-semibold text-primary">Lịch tuần</Link>} />
              {d ? (
                d.sessionsToday.length ? (
                  <ul className="divide-y divide-line border-t border-line">{d.sessionsToday.map((s) => <SessionRow key={s.id} s={s} showTeacher />)}</ul>
                ) : <EmptyState title="Hôm nay không có buổi học" description="Các buổi học sắp tới nằm trong Lịch." />
              ) : <div className="space-y-3 px-5 pb-5"><Skeleton className="h-10" /><Skeleton className="h-10" /></div>}
            </Card>
          </div>

          <Card>
            <CardHeader title="Học viên còn nợ nhiều nhất" action={<Link to="/app/billing?tab=debts" className="text-[13px] font-semibold text-primary">Tất cả công nợ</Link>} />
            {d ? (
              d.topDebts.length ? (
                <ul className="divide-y divide-line border-t border-line">
                  {d.topDebts.map((x) => (
                    <li key={x.studentId}>
                      <Link to={`/app/students/${x.studentId}?tab=billing`} className="flex items-center gap-3 px-4 py-3 hover:bg-surface-2 sm:px-5">
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium">{x.studentName}</p>
                          <p className="text-[12.5px] text-ink-2">{x.studentCode} · {x.invoiceCount} hóa đơn · hạn sớm nhất {formatDate(x.oldestDueDate)}</p>
                        </div>
                        <div className="tabular text-right">
                          <p className="font-semibold">{formatMoney(x.outstanding)}</p>
                          {x.overdueAmount > 0 && <p className="text-[12.5px] text-danger">{formatMoney(x.overdueAmount)} quá hạn</p>}
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : <EmptyState title="Không có công nợ" description="Tất cả hóa đơn đã được thanh toán." />
            ) : <div className="px-5 pb-5"><Skeleton className="h-24" /></div>}
          </Card>
        </div>
      )}
    </>
  );
}

function Stat({ label, value, sub }: { label: string; value?: string; sub?: React.ReactNode }) {
  return (
    <div className="px-4 py-4 sm:px-5">
      <p className="text-[13px] text-ink-2">{label}</p>
      {value ? <p className="tabular mt-1 text-[22px] font-bold leading-tight tracking-[-0.02em] sm:text-[26px]">{value}</p> : <Skeleton className="mt-2 h-7 w-28" />}
      <p className="mt-1 text-[12.5px] text-ink-2">{sub ?? " "}</p>
    </div>
  );
}
