import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Pencil, Plus } from "lucide-react";
import { api } from "@/api/client";
import type { Role, StaffUser } from "@/api/types";
import { Badge, Button, Card, EmptyState, ErrorState, Input, ListSkeleton, PageHeader, Select, Sheet } from "@/components/ui";
import { ROLE_LABEL } from "@/lib/labels";
import { formatDateTime } from "@/lib/format";
import { useAuth } from "@/lib/auth";

const schema = z.object({
  fullName: z.string().trim().min(2, "Vui lòng nhập họ tên"),
  email: z.string().trim().email("Email không hợp lệ"),
  phone: z.string().trim().regex(/^(0\d{9,10})?$/, "Số điện thoại không hợp lệ").optional(),
  role: z.enum(["CENTER_ADMIN", "STAFF", "TEACHER"]),
  password: z.string().optional(),
});
type Values = z.infer<typeof schema>;

export default function StaffPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [editing, setEditing] = useState<StaffUser | "new" | null>(null);
  const q = useQuery({ queryKey: ["staff"], queryFn: () => api<StaffUser[]>("/users") });
  const toggle = useMutation({
    mutationFn: (u: StaffUser) => api<StaffUser>(`/users/${u.id}/enabled`, { method: "PATCH", body: { enabled: !u.enabled } }),
    onSuccess: (u) => { qc.invalidateQueries({ queryKey: ["staff"] }); qc.invalidateQueries({ queryKey: ["teachers"] }); toast.success(u.enabled ? `Đã mở khóa ${u.fullName}` : `Đã khóa ${u.fullName}`); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <>
      <PageHeader title="Nhân viên" description="Tài khoản giáo vụ, thu ngân và giáo viên" actions={<Button icon={<Plus className="size-4" />} onClick={() => setEditing("new")}>Thêm nhân viên</Button>} />
      <Card>
        {q.isLoading ? <ListSkeleton /> : q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : !q.data!.length ? <EmptyState title="Chưa có nhân viên" /> : (
          <ul className="divide-y divide-line">
            {q.data!.map((u) => (
              <li key={u.id} className="flex flex-wrap items-center gap-3 px-4 py-3 sm:px-5">
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-2 truncate font-medium">{u.fullName}{u.id === user?.id && <Badge tone="primary">Bạn</Badge>}</p>
                  <p className="truncate text-[12.5px] text-ink-2">{u.email}{u.phone && ` · ${u.phone}`} · đăng nhập gần nhất {u.lastLoginAt ? formatDateTime(u.lastLoginAt) : "chưa có"}</p>
                </div>
                <Badge tone={u.role === "TEACHER" ? "info" : u.role === "CENTER_ADMIN" ? "primary" : "neutral"}>{ROLE_LABEL[u.role]}</Badge>
                {!u.enabled && <Badge tone="danger">Đã khóa</Badge>}
                <Button size="sm" variant="ghost" aria-label={`Sửa ${u.fullName}`} icon={<Pencil className="size-4" />} onClick={() => setEditing(u)} />
                {u.id !== user?.id && <Button size="sm" variant="secondary" loading={toggle.isPending && toggle.variables?.id === u.id} onClick={() => toggle.mutate(u)}>{u.enabled ? "Khóa" : "Mở khóa"}</Button>}
              </li>
            ))}
          </ul>
        )}
      </Card>
      <StaffSheet value={editing} onClose={() => setEditing(null)} />
    </>
  );
}

function StaffSheet({ value, onClose }: { value: StaffUser | "new" | null; onClose: () => void }) {
  const qc = useQueryClient();
  const u = value && value !== "new" ? value : null;
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    values: { fullName: u?.fullName ?? "", email: u?.email ?? "", phone: u?.phone ?? "", role: (u?.role as Values["role"]) ?? "TEACHER", password: "" },
  });
  const m = useMutation({
    mutationFn: (v: Values) => {
      if (!u && (!v.password || v.password.length < 8)) throw new Error("Mật khẩu tối thiểu 8 ký tự");
      const body = { fullName: v.fullName, email: v.email, phone: v.phone || null, role: v.role as Role, password: v.password || null };
      return u ? api(`/users/${u.id}`, { method: "PUT", body }) : api("/users", { method: "POST", body });
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["staff"] }); qc.invalidateQueries({ queryKey: ["teachers"] }); toast.success("Đã lưu nhân viên"); onClose(); },
    onError: (e) => { if (e.message.includes("Mật khẩu")) setError("password", { message: e.message }); toast.error(e.message); },
  });
  return (
    <Sheet open={!!value} onClose={onClose} title={u ? "Sửa nhân viên" : "Thêm nhân viên"}
      footer={<><Button variant="secondary" onClick={onClose}>Hủy</Button><Button type="submit" form="staff-form" loading={m.isPending}>Lưu</Button></>}>
      <form id="staff-form" noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4">
        <Input label="Họ tên" required error={errors.fullName?.message} {...register("fullName")} />
        <Input label="Email đăng nhập" type="email" required disabled={!!u} error={errors.email?.message} {...register("email")} />
        <Input label="Số điện thoại" inputMode="tel" error={errors.phone?.message} {...register("phone")} />
        <Select label="Vai trò" {...register("role")}>
          <option value="TEACHER">{ROLE_LABEL.TEACHER}</option>
          <option value="STAFF">{ROLE_LABEL.STAFF}</option>
          <option value="CENTER_ADMIN">{ROLE_LABEL.CENTER_ADMIN}</option>
        </Select>
        <Input label={u ? "Đặt lại mật khẩu (bỏ trống nếu giữ nguyên)" : "Mật khẩu tạm"} type="text" autoComplete="off" error={errors.password?.message} {...register("password")} />
      </form>
    </Sheet>
  );
}
