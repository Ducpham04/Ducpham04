import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, download } from "@/api/client";
import type { ClassRate, MonthValue } from "@/api/types";
import { Button, Card, CardBody, CardHeader, EmptyState, ListSkeleton, PageHeader, Select, Skeleton } from "@/components/ui";
import { addDays, formatMoney, formatMoneyShort, todayIso } from "@/lib/format";

function ExportButton({ path, name, query, children }: { path: string; name: string; query?: Record<string, string | number>; children: string }) {
  const [busy, setBusy] = useState(false);
  return (
    <Button variant="secondary" size="sm" icon={<Download className="size-4" />} loading={busy}
      onClick={async () => { setBusy(true); try { await download(path, name, query); } catch (e) { toast.error((e as Error).message); } finally { setBusy(false); } }}>
      {children}
    </Button>
  );
}

const axis = { tickLine: false, axisLine: false, tick: { fill: "var(--ink-2)", fontSize: 12 } } as const;
const tooltipStyle = { borderRadius: 10, border: "1px solid var(--line)", fontSize: 13 };

export default function ReportsPage() {
  const thisYear = Number(todayIso().slice(0, 4));
  const [year, setYear] = useState(thisYear);
  const [days, setDays] = useState(30);
  const to = todayIso();
  const from = addDays(to, -(days - 1));
  const revenue = useQuery({ queryKey: ["report", "revenue", year], queryFn: () => api<MonthValue[]>("/reports/revenue", { query: { year } }) });
  const newStudents = useQuery({ queryKey: ["report", "new-students", year], queryFn: () => api<MonthValue[]>("/reports/new-students", { query: { year } }) });
  const rates = useQuery({ queryKey: ["report", "attendance", from, to], queryFn: () => api<ClassRate[]>("/reports/attendance/classes", { query: { from, to } }) });
  const total = revenue.data?.reduce((a, m) => a + Number(m.value), 0) ?? 0;
  const totalNew = newStudents.data?.reduce((a, m) => a + Number(m.value), 0) ?? 0;

  return (
    <>
      <PageHeader title="Báo cáo" description="Doanh thu, học viên mới, chuyên cần – xuất Excel để gửi kế toán"
        actions={<Select aria-label="Chọn năm" value={year} onChange={(e) => setYear(Number(e.target.value))} wrapperClassName="w-28">
          {[thisYear, thisYear - 1, thisYear - 2].map((y) => <option key={y} value={y}>{y}</option>)}
        </Select>} />
      <div className="grid gap-5 lg:grid-cols-2">
        <Card>
          <CardHeader title={`Doanh thu ${year}`} description={revenue.data ? `Tổng ${formatMoney(total)}` : undefined}
            action={<ExportButton path="/reports/export/revenue.xlsx" name={`doanh-thu-${year}.xlsx`} query={{ year }}>Xuất Excel</ExportButton>} />
          <CardBody>
            {revenue.data ? (
              <div className="h-64">
                <ResponsiveContainer>
                  <BarChart data={revenue.data.map((m) => ({ name: `T${m.month}`, value: Number(m.value) }))} margin={{ left: -8, right: 4, top: 8 }}>
                    <CartesianGrid vertical={false} stroke="var(--line)" />
                    <XAxis dataKey="name" {...axis} />
                    <YAxis {...axis} tickFormatter={formatMoneyShort} width={56} />
                    <Tooltip cursor={{ fill: "var(--surface-2)" }} formatter={(v) => [formatMoney(Number(v)), "Doanh thu"]} contentStyle={tooltipStyle} />
                    <Bar dataKey="value" fill="var(--primary)" radius={[4, 4, 0, 0]} maxBarSize={32} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : <Skeleton className="h-64" />}
          </CardBody>
        </Card>
        <Card>
          <CardHeader title={`Học viên mới ${year}`} description={newStudents.data ? `Tổng ${totalNew} học viên` : undefined} />
          <CardBody>
            {newStudents.data ? (
              <div className="h-64">
                <ResponsiveContainer>
                  <BarChart data={newStudents.data.map((m) => ({ name: `T${m.month}`, value: Number(m.value) }))} margin={{ left: -24, right: 4, top: 8 }}>
                    <CartesianGrid vertical={false} stroke="var(--line)" />
                    <XAxis dataKey="name" {...axis} />
                    <YAxis {...axis} allowDecimals={false} />
                    <Tooltip cursor={{ fill: "var(--surface-2)" }} formatter={(v) => [v, "Học viên mới"]} contentStyle={tooltipStyle} />
                    <Bar dataKey="value" fill="var(--ok)" radius={[4, 4, 0, 0]} maxBarSize={32} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : <Skeleton className="h-64" />}
          </CardBody>
        </Card>
      </div>
      <Card className="mt-5">
        <CardHeader title="Chuyên cần theo lớp" description="Tỷ lệ = (có mặt + đi muộn) / tổng lượt điểm danh"
          action={<div className="flex items-center gap-2">
            <Select aria-label="Khoảng thời gian" value={days} onChange={(e) => setDays(Number(e.target.value))} wrapperClassName="w-36">
              <option value={7}>7 ngày qua</option><option value={30}>30 ngày qua</option><option value={90}>90 ngày qua</option>
            </Select>
            <ExportButton path="/reports/export/attendance.xlsx" name="chuyen-can.xlsx" query={{ from, to }}>Excel</ExportButton>
          </div>} />
        {rates.isLoading ? <ListSkeleton /> : !rates.data?.length ? <EmptyState title="Chưa có dữ liệu điểm danh trong khoảng này" /> : (
          <div className="overflow-x-auto border-t border-line">
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead className="bg-surface-2 text-[13px] text-ink-2">
                <tr><th className="px-5 py-2.5 font-medium">Lớp</th><th className="px-3 py-2.5 text-right font-medium">Có mặt</th><th className="px-3 py-2.5 text-right font-medium">Muộn</th><th className="px-3 py-2.5 text-right font-medium">Có phép</th><th className="px-3 py-2.5 text-right font-medium">Vắng</th><th className="w-48 px-5 py-2.5 font-medium">Tỷ lệ</th></tr>
              </thead>
              <tbody className="tabular divide-y divide-line">
                {rates.data.map((r) => (
                  <tr key={r.classId}>
                    <td className="px-5 py-2.5"><p className="font-medium">{r.classCode}</p><p className="text-[12.5px] text-ink-2">{r.className}</p></td>
                    <td className="px-3 py-2.5 text-right">{r.present}</td><td className="px-3 py-2.5 text-right">{r.late}</td>
                    <td className="px-3 py-2.5 text-right">{r.excused}</td><td className="px-3 py-2.5 text-right">{r.absent}</td>
                    <td className="px-5 py-2.5">
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 flex-1 rounded-full bg-surface-2" aria-hidden><div className={r.rate >= 85 ? "h-1.5 rounded-full bg-ok" : r.rate >= 70 ? "h-1.5 rounded-full bg-warn" : "h-1.5 rounded-full bg-danger"} style={{ width: `${r.rate}%` }} /></div>
                        <span className="w-12 text-right font-semibold">{r.rate.toLocaleString("vi-VN")}%</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
