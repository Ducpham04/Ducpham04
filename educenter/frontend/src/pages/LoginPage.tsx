import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { CalendarCheck2, ClipboardCheck, QrCode } from "lucide-react";
import { Button, Input } from "@/components/ui";
import { homePathFor, useAuth } from "@/lib/auth";
import { ROLE_LABEL } from "@/lib/labels";
import type { Role } from "@/api/types";

const schema = z.object({
  email: z.string().trim().min(1, "Vui lòng nhập email").email("Email không hợp lệ"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});
type FormValues = z.infer<typeof schema>;

export const DEMO_ACCOUNTS: { email: string; password: string; role: Role }[] = [
  { email: "chulan@hoasen.edu.vn", password: "Demo@123", role: "CENTER_ADMIN" },
  { email: "giaovu@hoasen.edu.vn", password: "Demo@123", role: "STAFF" },
  { email: "david@hoasen.edu.vn", password: "Demo@123", role: "TEACHER" },
  { email: "phuhuynh@hoasen.edu.vn", password: "Demo@123", role: "STUDENT" },
  { email: "admin@educenter.vn", password: "Admin@123", role: "SUPER_ADMIN" },
];

const BENEFITS = [
  { icon: CalendarCheck2, text: "Xếp lịch lớp không lo trùng phòng, trùng giáo viên" },
  { icon: ClipboardCheck, text: "Giáo viên điểm danh trên điện thoại trong một phút" },
  { icon: QrCode, text: "Thu học phí nhiều đợt, mã VietQR, phiếu thu PDF" },
];

export function LoginPage() {
  const { user, ready, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(schema) });

  if (ready && user) return <Navigate to={homePathFor(user.role)} replace />;

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    try {
      const me = await login(values.email, values.password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== "/login" ? from : homePathFor(me.role), { replace: true });
    } catch (e) {
      setServerError(e instanceof Error ? e.message : "Đăng nhập không thành công");
    }
  });

  return (
    <div className="grid min-h-dvh lg:grid-cols-[1.05fr_1fr]">
      <section className="hidden flex-col justify-between bg-primary-strong px-12 py-10 text-white lg:flex">
        <div className="flex items-center gap-2.5 text-lg font-bold">
          <img src="/favicon.svg" alt="" className="size-9 rounded-lg ring-1 ring-white/30" />
          EduCenter
        </div>
        <div className="max-w-md">
          <h1 className="text-[34px] font-bold leading-[1.15] tracking-[-0.025em]">Vận hành trung tâm tiếng Anh gọn trong một màn hình.</h1>
          <ul className="mt-8 space-y-4">
            {BENEFITS.map((b) => (
              <li key={b.text} className="flex items-start gap-3 text-[15px] text-white/90">
                <b.icon className="mt-0.5 size-5 shrink-0 text-white/80" strokeWidth={1.75} aria-hidden />
                {b.text}
              </li>
            ))}
          </ul>
        </div>
        <p className="text-[13px] text-white/60">Dành cho trung tâm 50 – 1.000 học viên tại Việt Nam.</p>
      </section>

      <section className="flex items-center justify-center px-4 py-10 sm:px-8">
        <div className="w-full max-w-[400px]">
          <div className="mb-8 flex items-center gap-2.5 text-lg font-bold lg:hidden">
            <img src="/favicon.svg" alt="" className="size-9" />
            EduCenter
          </div>
          <h2 className="text-2xl font-bold tracking-[-0.02em]">Đăng nhập</h2>
          <p className="mt-1 text-sm text-ink-2">Dùng email trung tâm đã cấp cho bạn.</p>

          <form onSubmit={onSubmit} noValidate className="mt-6 space-y-4">
            {serverError && <p role="alert" className="rounded-lg bg-danger-soft px-3 py-2.5 text-sm text-danger">{serverError}</p>}
            <Input label="Email" type="email" autoComplete="username" inputMode="email" error={errors.email?.message} {...register("email")} />
            <Input label="Mật khẩu" type="password" autoComplete="current-password" error={errors.password?.message} {...register("password")} />
            <Button type="submit" size="lg" className="w-full" loading={isSubmitting}>Đăng nhập</Button>
          </form>

          <div className="mt-8 rounded-xl border border-line bg-surface p-4">
            <p className="text-[13px] font-semibold text-ink">Tài khoản demo</p>
            <p className="mt-0.5 text-[12.5px] text-ink-2">Chạm để điền sẵn, rồi bấm Đăng nhập.</p>
            <ul className="mt-3 divide-y divide-line">
              {DEMO_ACCOUNTS.map((a) => (
                <li key={a.email}>
                  <button
                    type="button"
                    onClick={() => { setValue("email", a.email); setValue("password", a.password); }}
                    className="flex w-full items-center justify-between gap-3 py-2.5 text-left hover:text-primary"
                  >
                    <span className="truncate text-[13.5px]">{a.email}</span>
                    <span className="shrink-0 text-[12px] text-ink-2">{ROLE_LABEL[a.role]}</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
