import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { LogOut } from "lucide-react";
import { api } from "@/api/client";
import { Button, Card, CardBody, CardHeader, Input, PageHeader } from "@/components/ui";
import { useAuth } from "@/lib/auth";
import { ROLE_LABEL } from "@/lib/labels";

const schema = z.object({
  currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
  newPassword: z.string().min(8, "Mật khẩu mới tối thiểu 8 ký tự").max(72),
  confirm: z.string(),
}).refine((v) => v.newPassword === v.confirm, { message: "Mật khẩu nhập lại không khớp", path: ["confirm"] });
type Values = z.infer<typeof schema>;

export function ChangePasswordCard({ onDone }: { onDone?: () => void }) {
  const { logout } = useAuth();
  const { register, handleSubmit, reset, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema) });
  const m = useMutation({
    mutationFn: (v: Values) => api("/auth/change-password", { method: "POST", body: { currentPassword: v.currentPassword, newPassword: v.newPassword } }),
    onSuccess: async () => {
      reset();
      toast.success("Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
      onDone?.();
      await logout();
    },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Card>
      <CardHeader title="Đổi mật khẩu" description="Sau khi đổi, mọi thiết bị khác sẽ bị đăng xuất." />
      <CardBody>
        <form noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid max-w-sm gap-4">
          <Input label="Mật khẩu hiện tại" type="password" autoComplete="current-password" error={errors.currentPassword?.message} {...register("currentPassword")} />
          <Input label="Mật khẩu mới" type="password" autoComplete="new-password" error={errors.newPassword?.message} {...register("newPassword")} />
          <Input label="Nhập lại mật khẩu mới" type="password" autoComplete="new-password" error={errors.confirm?.message} {...register("confirm")} />
          <div><Button type="submit" loading={m.isPending}>Đổi mật khẩu</Button></div>
        </form>
      </CardBody>
    </Card>
  );
}

export default function AccountPage() {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <>
      <PageHeader title="Tài khoản" description={`${user.fullName} · ${ROLE_LABEL[user.role]}`} actions={<Button variant="secondary" icon={<LogOut className="size-4" />} onClick={logout}>Đăng xuất</Button>} />
      <div className="grid gap-5 lg:grid-cols-2">
        <Card>
          <CardHeader title="Thông tin" />
          <CardBody>
            <dl className="divide-y divide-line text-sm">
              <div className="flex justify-between gap-3 py-2.5"><dt className="text-ink-2">Email</dt><dd className="truncate">{user.email}</dd></div>
              <div className="flex justify-between gap-3 py-2.5"><dt className="text-ink-2">Vai trò</dt><dd>{ROLE_LABEL[user.role]}</dd></div>
              {user.tenantName && <div className="flex justify-between gap-3 py-2.5"><dt className="text-ink-2">Trung tâm</dt><dd className="truncate">{user.tenantName}</dd></div>}
            </dl>
          </CardBody>
        </Card>
        <ChangePasswordCard />
      </div>
    </>
  );
}
