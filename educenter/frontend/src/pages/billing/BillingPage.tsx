import { useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Download, Pencil, Plus } from "lucide-react";
import { api, download } from "@/api/client";
import { useCourses, useFeePlans } from "@/api/hooks";
import type { DebtSummary, FeePlan, Invoice, Page } from "@/api/types";
import { Badge, Button, Card, CardHeader, EmptyState, ErrorState, Input, ListSkeleton, PageHeader, Pagination, SearchInput, Select, Sheet, Tabs, Textarea } from "@/components/ui";
import { InvoiceList } from "@/components/domain/InvoiceList";
import { formatDate, formatMoney } from "@/lib/format";
import { InvoiceFormSheet } from "./InvoiceForm";

type Tab = "invoices" | "debts" | "plans";

export default function BillingPage() {
  const [params, setParams] = useSearchParams();
  const tab = (params.get("tab") as Tab) ?? "invoices";
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Học phí" description="Hóa đơn, thanh toán nhiều lần và công nợ" actions={<Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Lập hóa đơn</Button>} />
      <Tabs<Tab> value={tab} onChange={(t) => setParams({ tab: t }, { replace: true })} className="mb-5"
        items={[{ value: "invoices", label: "Hóa đơn" }, { value: "debts", label: "Công nợ" }, { value: "plans", label: "Gói học phí" }]} />
      {tab === "invoices" && <InvoicesTab />}
      {tab === "debts" && <DebtsTab />}
      {tab === "plans" && <PlansTab />}
      <InvoiceFormSheet open={creating} onClose={() => setCreating(false)} onCreated={(i) => navigate(`/app/billing/invoices/${i.id}`)} />
    </>
  );
}

function InvoicesTab() {
  const [status, setStatus] = useState("OPEN");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ["invoices", status, q, page],
    queryFn: () => api<Page<Invoice>>("/invoices", { query: { status: status === "ALL" ? undefined : status, q, page, size: 20 } }),
    placeholderData: keepPreviousData,
  });
  return (
    <Card>
      <div className="flex flex-col gap-3 px-4 pt-4 sm:flex-row sm:items-end sm:px-5">
        <SearchInput value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Mã hóa đơn hoặc tên học viên" />
        <Select aria-label="Lọc trạng thái" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} wrapperClassName="sm:w-48">
          <option value="OPEN">Chưa thu đủ</option>
          <option value="OVERDUE">Quá hạn</option>
          <option value="PAID">Đã thu đủ</option>
          <option value="CANCELLED">Đã hủy</option>
          <option value="ALL">Tất cả</option>
        </Select>
      </div>
      <div className="mt-3 border-t border-line">
        {query.isLoading ? <ListSkeleton /> : query.isError ? <ErrorState error={query.error} onRetry={() => query.refetch()} /> : query.data!.items.length === 0 ? (
          <EmptyState title="Không có hóa đơn" description="Thử đổi bộ lọc trạng thái." />
        ) : (
          <>
            <InvoiceList invoices={query.data!.items} />
            <Pagination page={query.data!.page} totalPages={query.data!.totalPages} totalItems={query.data!.totalItems} onChange={setPage} />
          </>
        )}
      </div>
    </Card>
  );
}

function DebtsTab() {
  const q = useQuery({ queryKey: ["debts"], queryFn: () => api<DebtSummary>("/debts") });
  const [exporting, setExporting] = useState(false);
  if (q.isLoading) return <Card><ListSkeleton /></Card>;
  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>;
  const d = q.data!;
  return (
    <div className="space-y-5">
      <Card className="grid grid-cols-3 divide-x divide-line">
        <div className="px-4 py-4 sm:px-5"><p className="text-[13px] text-ink-2">Tổng công nợ</p><p className="tabular mt-1 text-xl font-bold sm:text-2xl">{formatMoney(d.totalOutstanding)}</p></div>
        <div className="px-4 py-4 sm:px-5"><p className="text-[13px] text-ink-2">Quá hạn</p><p className="tabular mt-1 text-xl font-bold text-danger sm:text-2xl">{formatMoney(d.totalOverdue)}</p></div>
        <div className="px-4 py-4 sm:px-5"><p className="text-[13px] text-ink-2">Học viên còn nợ</p><p className="tabular mt-1 text-xl font-bold sm:text-2xl">{d.studentCount}</p></div>
      </Card>
      <Card>
        <CardHeader title="Danh sách công nợ" description="Sắp xếp theo số tiền quá hạn"
          action={<Button variant="secondary" size="sm" icon={<Download className="size-4" />} loading={exporting}
            onClick={async () => { setExporting(true); try { await download("/reports/export/debts.xlsx", "cong-no.xlsx"); } catch (e) { toast.error((e as Error).message); } finally { setExporting(false); } }}>Xuất Excel</Button>} />
        {d.items.length === 0 ? <EmptyState title="Không có công nợ" description="Mọi hóa đơn đã được thanh toán đủ." /> : (
          <ul className="divide-y divide-line border-t border-line">
            {d.items.map((x) => (
              <li key={x.studentId}>
                <Link to={`/app/students/${x.studentId}?tab=billing`} className="flex items-center gap-3 px-4 py-3 hover:bg-surface-2 sm:px-5">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{x.studentName}</p>
                    <p className="tabular truncate text-[12.5px] text-ink-2">{x.studentCode} · {x.guardianPhone ?? x.phone ?? "chưa có SĐT"} · hạn sớm nhất {formatDate(x.oldestDueDate)}</p>
                  </div>
                  {x.overdueAmount > 0 && <span className="hidden sm:block"><Badge tone="danger">Quá hạn {formatMoney(x.overdueAmount)}</Badge></span>}
                  <p className="tabular w-32 text-right font-semibold">{formatMoney(x.outstanding)}</p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

const planSchema = z.object({
  name: z.string().trim().min(2, "Vui lòng nhập tên gói"),
  courseId: z.string().optional(),
  amount: z.string().refine((v) => Number(v.replace(/\D/g, "")) >= 1000, "Số tiền tối thiểu 1.000 ₫"),
  sessions: z.string().optional(),
  description: z.string().max(500).optional(),
  active: z.boolean(),
});
type PlanValues = z.infer<typeof planSchema>;

function PlansTab() {
  const plans = useFeePlans();
  const [editing, setEditing] = useState<FeePlan | "new" | null>(null);
  return (
    <Card>
      <CardHeader title="Gói học phí" description="Dùng khi lập hóa đơn để điền sẵn số tiền" action={<Button size="sm" icon={<Plus className="size-4" />} onClick={() => setEditing("new")}>Thêm gói</Button>} />
      {plans.isLoading ? <ListSkeleton /> : !plans.data?.length ? <EmptyState title="Chưa có gói học phí" /> : (
        <ul className="divide-y divide-line border-t border-line">
          {plans.data.map((p) => (
            <li key={p.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{p.name}</p>
                <p className="truncate text-[12.5px] text-ink-2">{p.courseName ?? "Không gắn khóa"}{p.sessions ? ` · ${p.sessions} buổi` : ""}{p.description ? ` · ${p.description}` : ""}</p>
              </div>
              {!p.active && <Badge>Ngừng dùng</Badge>}
              <p className="tabular font-semibold">{formatMoney(p.amount)}</p>
              <Button size="sm" variant="ghost" aria-label={`Sửa ${p.name}`} icon={<Pencil className="size-4" />} onClick={() => setEditing(p)} />
            </li>
          ))}
        </ul>
      )}
      <PlanSheet value={editing} onClose={() => setEditing(null)} />
    </Card>
  );
}

function PlanSheet({ value, onClose }: { value: FeePlan | "new" | null; onClose: () => void }) {
  const qc = useQueryClient();
  const courses = useCourses();
  const p = value && value !== "new" ? value : null;
  const { register, handleSubmit, formState: { errors } } = useForm<PlanValues>({
    resolver: zodResolver(planSchema),
    values: { name: p?.name ?? "", courseId: p?.courseId ? String(p.courseId) : "", amount: p ? String(p.amount) : "", sessions: p?.sessions ? String(p.sessions) : "", description: p?.description ?? "", active: p?.active ?? true },
  });
  const m = useMutation({
    mutationFn: (v: PlanValues) => {
      const body = { name: v.name, courseId: v.courseId ? Number(v.courseId) : null, amount: Number(v.amount.replace(/\D/g, "")), sessions: v.sessions ? Number(v.sessions) : null, description: v.description || null, active: v.active };
      return p ? api(`/fee-plans/${p.id}`, { method: "PUT", body }) : api("/fee-plans", { method: "POST", body });
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["fee-plans"] }); toast.success("Đã lưu gói học phí"); onClose(); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Sheet open={!!value} onClose={onClose} title={p ? "Sửa gói học phí" : "Thêm gói học phí"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="plan-form" loading={m.isPending}>Lưu</Button></>}>
      <form id="plan-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
        <Input label="Tên gói" required error={errors.name?.message} {...register("name")} />
        <Select label="Khóa học" {...register("courseId")}>
          <option value="">— Không gắn khóa —</option>
          {courses.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </Select>
        <div className="grid grid-cols-2 gap-4">
          <Input label="Số tiền (₫)" required inputMode="numeric" error={errors.amount?.message} {...register("amount")} />
          <Input label="Số buổi" type="number" min={0} {...register("sessions")} />
        </div>
        <Textarea label="Mô tả" rows={2} {...register("description")} />
        <label className="flex items-center gap-2.5 text-sm"><input type="checkbox" className="size-5 accent-[var(--primary)]" {...register("active")} />Đang sử dụng</label>
      </form>
    </Sheet>
  );
}
