import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Mail, MessageCircle, Play } from "lucide-react";
import { api } from "@/api/client";
import { useBanks, useCenter } from "@/api/hooks";
import type { NotificationLogItem, Page, Tenant } from "@/api/types";
import { Badge, Button, Card, CardBody, CardHeader, EmptyState, Input, ListSkeleton, PageHeader, Select, Skeleton } from "@/components/ui";
import { formatDateTime, formatMoney } from "@/lib/format";
import { PLAN_LABEL } from "@/lib/labels";

const schema = z.object({
  name: z.string().trim().min(2, "Vui lòng nhập tên trung tâm"),
  phone: z.string().trim().regex(/^(0\d{9,10})?$/, "Số điện thoại không hợp lệ").optional(),
  email: z.union([z.literal(""), z.string().email("Email không hợp lệ")]).optional(),
  address: z.string().max(255).optional(),
  bankCode: z.string().optional(),
  bankAccountNo: z.string().regex(/^([0-9]{6,20})?$/, "Số tài khoản 6–20 chữ số").optional(),
  bankAccountName: z.string().max(100).optional(),
});
type Values = z.infer<typeof schema>;

export default function SettingsPage() {
  const center = useCenter();
  const banks = useBanks();
  return (
    <>
      <PageHeader title="Cài đặt" description={center.data && `Gói ${PLAN_LABEL[center.data.plan]} · mã trung tâm ${center.data.slug}`} />
      <div className="grid gap-5 lg:grid-cols-2">
        {center.data && banks.data ? <CenterForm center={center.data} banks={banks.data} /> : <Skeleton className="h-96" />}
        <Reminders />
      </div>
    </>
  );
}

function CenterForm({ center, banks }: { center: Tenant; banks: Record<string, string> }) {
  const qc = useQueryClient();
  const { register, handleSubmit, watch, formState: { errors, isDirty } } = useForm<Values>({
    resolver: zodResolver(schema),
    values: { name: center.name, phone: center.phone ?? "", email: center.email ?? "", address: center.address ?? "", bankCode: center.bankCode ?? "", bankAccountNo: center.bankAccountNo ?? "", bankAccountName: center.bankAccountName ?? "" },
  });
  const m = useMutation({
    mutationFn: (v: Values) => api<Tenant>("/center", { method: "PUT", body: Object.fromEntries(Object.entries(v).map(([k, x]) => [k, x === "" ? null : x])) }),
    onSuccess: (t) => { qc.setQueryData(["center"], t); toast.success("Đã lưu thông tin trung tâm"); },
    onError: (e) => toast.error(e.message),
  });
  const [bankCode, accNo, accName] = watch(["bankCode", "bankAccountNo", "bankAccountName"]);
  const preview = bankCode && accNo && accNo.length >= 6
    ? `https://img.vietqr.io/image/${bankCode}-${accNo}-compact2.png?amount=100000&addInfo=${encodeURIComponent("HP THU NGHIEM")}&accountName=${encodeURIComponent(accName ?? "")}`
    : null;
  return (
    <Card>
      <CardHeader title="Thông tin trung tâm & ngân hàng" description="Hiển thị trên phiếu thu, email nhắc và mã VietQR" />
      <CardBody>
        <form noValidate onSubmit={handleSubmit((v) => m.mutate(v))} className="grid gap-4 sm:grid-cols-2">
          <Input label="Tên trung tâm" required wrapperClassName="sm:col-span-2" error={errors.name?.message} {...register("name")} />
          <Input label="Điện thoại" inputMode="tel" error={errors.phone?.message} {...register("phone")} />
          <Input label="Email" type="email" error={errors.email?.message} {...register("email")} />
          <Input label="Địa chỉ" wrapperClassName="sm:col-span-2" {...register("address")} />
          <fieldset className="grid gap-4 border-t border-line pt-4 sm:col-span-2 sm:grid-cols-2">
            <legend className="mb-1 text-[15px] font-semibold">Tài khoản nhận học phí (VietQR)</legend>
            <Select label="Ngân hàng" {...register("bankCode")}>
              <option value="">— Chọn ngân hàng —</option>
              {Object.entries(banks).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
            </Select>
            <Input label="Số tài khoản" inputMode="numeric" error={errors.bankAccountNo?.message} {...register("bankAccountNo")} />
            <Input label="Tên chủ tài khoản" wrapperClassName="sm:col-span-2" hint="Viết hoa không dấu như trên app ngân hàng" {...register("bankAccountName")} />
            {preview && (
              <div className="flex items-center gap-4 rounded-lg bg-surface-2 p-3 sm:col-span-2">
                <img src={preview} alt="Xem thử mã QR" className="size-28 rounded-lg bg-white p-1" />
                <p className="text-[13px] text-ink-2">Xem thử QR chuyển {formatMoney(100000)}. Quét bằng app ngân hàng để kiểm tra đúng tên chủ tài khoản trước khi gửi phụ huynh.</p>
              </div>
            )}
          </fieldset>
          <div className="sm:col-span-2"><Button type="submit" loading={m.isPending} disabled={!isDirty}>Lưu thay đổi</Button></div>
        </form>
      </CardBody>
    </Card>
  );
}

function Reminders() {
  const qc = useQueryClient();
  const logs = useQuery({ queryKey: ["notif-logs"], queryFn: () => api<Page<NotificationLogItem>>("/notifications/logs", { query: { size: 30 } }) });
  const run = useMutation({
    mutationFn: (kind: "sessions" | "fees") => api<{ sent: number; failed: number; skipped: number }>(`/notifications/run/${kind}`, { method: "POST" }),
    onSuccess: (r) => { qc.invalidateQueries({ queryKey: ["notif-logs"] }); toast.success(`Đã gửi ${r.sent} email · lỗi ${r.failed} · bỏ qua ${r.skipped} (đã gửi trước đó)`); },
    onError: (e) => toast.error(e.message),
  });
  return (
    <Card>
      <CardHeader title="Nhắc nhở tự động" description="Email nhắc lịch học lúc 19:00 cho buổi ngày mai; nhắc học phí lúc 08:00 cho hóa đơn còn ≤ 3 ngày hoặc quá hạn." />
      <CardBody className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Play className="size-4" />} loading={run.isPending && run.variables === "sessions"} onClick={() => run.mutate("sessions")}>Gửi nhắc lịch ngay</Button>
          <Button variant="secondary" size="sm" icon={<Play className="size-4" />} loading={run.isPending && run.variables === "fees"} onClick={() => run.mutate("fees")}>Gửi nhắc học phí ngay</Button>
        </div>
        <p className="rounded-lg bg-info-soft px-3 py-2.5 text-[13px] text-info">
          Zalo ZNS đang chạy chế độ thử (mock): tin được ghi nhật ký, chưa gửi thật. Cần Zalo OA đã xác thực và template được duyệt để bật – xem README.
        </p>
      </CardBody>
      <div className="border-t border-line">
        <p className="px-4 pt-3 text-[13px] font-semibold sm:px-5">Nhật ký gửi gần đây</p>
        {logs.isLoading ? <ListSkeleton rows={4} /> : !logs.data?.items.length ? <EmptyState title="Chưa gửi nhắc nhở nào" /> : (
          <ul className="max-h-[420px] divide-y divide-line overflow-y-auto">
            {logs.data.items.map((l) => (
              <li key={l.id} className="flex items-start gap-3 px-4 py-2.5 sm:px-5">
                {l.channel === "EMAIL" ? <Mail className="mt-0.5 size-4 shrink-0 text-ink-2" aria-label="Email" /> : <MessageCircle className="mt-0.5 size-4 shrink-0 text-ink-2" aria-label="Zalo" />}
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm">{l.subject}</p>
                  <p className="truncate text-[12.5px] text-ink-2">{l.recipient} · {formatDateTime(l.createdAt)}{l.error && ` · ${l.error}`}</p>
                </div>
                <Badge tone={l.status === "SENT" ? "ok" : l.status === "MOCK" ? "info" : l.status === "FAILED" ? "danger" : "neutral"}>
                  {l.status === "SENT" ? "Đã gửi" : l.status === "MOCK" ? "Thử (mock)" : l.status === "FAILED" ? "Lỗi" : "Bỏ qua"}
                </Badge>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  );
}
