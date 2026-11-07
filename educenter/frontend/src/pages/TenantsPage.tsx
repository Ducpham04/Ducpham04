import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { api } from "@/api/client";
import type { Tenant, TenantPlan } from "@/api/types";
import { Badge, Button, Card, EmptyState, ErrorState, Input, ListSkeleton, PageHeader, Select, Sheet } from "@/components/ui";
import { PLAN_LABEL } from "@/lib/labels";
import { formatDate } from "@/lib/format";

const schema = z.object({
  name: z.string().trim().min(2, "Vui lòng nhập tên trung tâm"),
  slug: z.string().trim().regex(/^[a-z0-9-]{3,60}$/, "Chữ thường không dấu, số, gạch ngang (3–60 ký tự)"),
  phone: z.string().optional(),
  email: z.union([z.literal(""), z.string().email("Email không hợp lệ")]).optional(),
  address: z.string().optional(),
  plan: z.enum(["TRIAL", "BASIC", "PRO", "LIFETIME"]),
  ownerName: z.string().trim().min(2, "Vui lòng nhập họ tên chủ trung tâm"),
  ownerEmail: z.string().trim().email("Email không hợp lệ"),
  ownerPassword: z.string().min(8, "Mật khẩu tối thiểu 8 ký tự"),
});
type Values = z.infer<typeof schema>;

export default function TenantsPage() {
  const qc = useQueryClient();
  const [creating, setCreating] = useState(false);
  const q = useQuery({ queryKey: ["tenants"], queryFn: () => api<Tenant[]>("/admin/tenants") });
  const toggle = useMutation({
    mutationFn: (t: Tenant) => api<Tenant>(`/admin/tenants/${t.id}/status`, { method: "PATCH", body: { status: t.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE" } }),
    onSuccess: (t) => { qc.invalidateQueries({ queryKey: ["tenants"] }); toast.success(t.status === "ACTIVE" ? `Đã mở khóa ${t.name}` : `Đã tạm khóa ${t.name}`); },
    onError: (e) => toast.error(e.message),
  });
  const total = q.data?.reduce((a, t) => a + (t.studentCount ?? 0), 0) ?? 0;
  return (
    <>
      <PageHeader title="Trung tâm" description={q.data ? `${q.data.length} trung tâm · ${total} học viên trên hệ thống` : "Quản lý khách hàng EduCenter"}
        actions={<Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Tạo trung tâm</Button>} />
      <Card>
        {q.isLoading ? <ListSkeleton /> : q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : !q.data!.length ? <EmptyState title="Chưa có trung tâm" /> : (
          <ul className="divide-y divide-line">
            {q.data!.map((t) => (
              <li key={t.id} className="flex flex-wrap items-center gap-3 px-4 py-3 sm:px-5">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{t.name}</p>
                  <p className="truncate text-[12.5px] text-ink-2">{t.slug} · {t.email ?? "—"} · tạo {formatDate(t.createdAt.slice(0, 10))}</p>
                </div>
                <p className="tabular text-[13px] text-ink-2">{t.studentCount} HV · {t.userCount} tài khoản</p>
                <Badge tone={t.plan === "PRO" || t.plan === "LIFETIME" ? "primary" : "neutral"}>{PLAN_LABEL[t.plan]}</Badge>
                <Badge tone={t.status === "ACTIVE" ? "ok" : "danger"}>{t.status === "ACTIVE" ? "Hoạt động" : "Tạm khóa"}</Badge>
                <Button size="sm" variant="secondary" loading={toggle.isPending && toggle.variables?.id === t.id} onClick={() => toggle.mutate(t)}>{t.status === "ACTIVE" ? "Tạm khóa" : "Mở khóa"}</Button>
              </li>
            ))}
          </ul>
        )}
      </Card>
      <CreateTenantSheet open={creating} onClose={() => setCreating(false)} />
    </>
  );
}

function CreateTenantSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const qc = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { plan: "TRIAL" } });
  const m = useMutation({
    mutationFn: (v: Values) => api<Tenant>("/admin/tenants", {
      method: "POST",
      body: { name: v.name, slug: v.slug, phone: v.phone || null, email: v.email || null, address: v.address || null, plan: v.plan as TenantPlan, owner: { fullName: v.ownerName, email: v.ownerEmail, password: v.ownerPassword } },
    }),
    onSuccess: (t) => { qc.invalidateQueries({ queryKey: ["tenants"] }); toast.success(`Đã tạo trung tâm ${t.name}`); reset(); onClose(); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Sheet open={open} onClose={onClose} title="Tạo trung tâm mới" description="Kèm tài khoản chủ trung tâm để bàn giao"
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="tenant-form" loading={m.isPending}>Tạo trung tâm</Button></>}>
      <form id="tenant-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
        <Input label="Tên trung tâm" required error={errors.name?.message} {...register("name")} />
        <Input label="Mã trung tâm" required placeholder="vd: anh-ngu-binh-minh" error={errors.slug?.message} {...register("slug")} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Điện thoại" {...register("phone")} />
          <Select label="Gói" {...register("plan")}>{Object.entries(PLAN_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</Select>
        </div>
        <Input label="Email trung tâm" type="email" error={errors.email?.message} {...register("email")} />
        <Input label="Địa chỉ" {...register("address")} />
        <fieldset className="grid gap-4 border-t border-line pt-4">
          <legend className="mb-1 text-[15px] font-semibold">Tài khoản chủ trung tâm</legend>
          <Input label="Họ tên" required error={errors.ownerName?.message} {...register("ownerName")} />
          <Input label="Email đăng nhập" type="email" required error={errors.ownerEmail?.message} {...register("ownerEmail")} />
          <Input label="Mật khẩu tạm" type="text" autoComplete="off" required error={errors.ownerPassword?.message} {...register("ownerPassword")} />
        </fieldset>
      </form>
    </Sheet>
  );
}
