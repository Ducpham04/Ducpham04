import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { FileText, XCircle } from "lucide-react";
import { api, download, ApiError } from "@/api/client";
import type { Invoice, PaymentMethod, VietQr } from "@/api/types";
import { Button, Card, CardBody, CardHeader, ConfirmDialog, EmptyState, ErrorState, Input, ListSkeleton, PageHeader, Select, Skeleton, StatusChip } from "@/components/ui";
import { QrPanel } from "@/components/domain/QrPanel";
import { INVOICE_STATUS, PAYMENT_METHOD, invoiceStatusKey } from "@/lib/labels";
import { formatDate, formatDateTime, formatMoney } from "@/lib/format";

const paySchema = z.object({
  amount: z.string().refine((v) => Number(v.replace(/\D/g, "")) >= 1000, "Số tiền tối thiểu 1.000 ₫"),
  method: z.enum(["CASH", "BANK_TRANSFER", "CARD", "OTHER"]),
  note: z.string().max(255).optional(),
});
type PayValues = z.infer<typeof paySchema>;

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const qc = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const q = useQuery({ queryKey: ["invoice", id], queryFn: () => api<Invoice>(`/invoices/${id}`) });
  const inv = q.data;
  const open = inv && (inv.status === "UNPAID" || inv.status === "PARTIAL");
  const qr = useQuery({ queryKey: ["invoice", id, "qr", inv?.remaining], queryFn: () => api<VietQr>(`/invoices/${id}/vietqr`), enabled: !!open, retry: false });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<PayValues>({
    resolver: zodResolver(paySchema),
    values: { amount: inv ? String(inv.remaining) : "", method: "CASH", note: "" },
  });
  const refresh = (data: Invoice) => {
    qc.setQueryData(["invoice", id], data);
    qc.invalidateQueries({ queryKey: ["invoices"] });
    qc.invalidateQueries({ queryKey: ["debts"] });
    qc.invalidateQueries({ queryKey: ["dashboard"] });
    qc.invalidateQueries({ queryKey: ["student", String(data.studentId)] });
  };
  const pay = useMutation({
    mutationFn: (v: PayValues) => api<Invoice>(`/invoices/${id}/payments`, { method: "POST", body: { amount: Number(v.amount.replace(/\D/g, "")), method: v.method, note: v.note || null } }),
    onSuccess: (data) => {
      refresh(data);
      reset();
      const last = data.payments?.[data.payments.length - 1];
      toast.success(`Đã ghi nhận ${formatMoney(last?.amount)} – phiếu thu ${last?.receiptNo}`, {
        action: last ? { label: "In phiếu thu", onClick: () => download(`/payments/${last.id}/receipt.pdf`, `${last.receiptNo}.pdf`, undefined, true) } : undefined,
      });
    },
    onError: (e) => toast.error(e.message),
  });
  const cancel = useMutation({
    mutationFn: () => api<Invoice>(`/invoices/${id}/cancel`, { method: "POST", body: { reason: "Hủy bởi giáo vụ" } }),
    onSuccess: (data) => { refresh(data); setCancelOpen(false); toast.success("Đã hủy hóa đơn"); },
    onError: (e) => toast.error(e.message),
  });

  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>;

  return (
    <>
      <PageHeader
        back={{ to: "/app/billing", label: "Học phí" }}
        title={inv ? `Hóa đơn ${inv.code}` : <Skeleton className="h-8 w-48" />}
        description={inv && <span className="inline-flex flex-wrap items-center gap-2"><Link to={`/app/students/${inv.studentId}?tab=billing`} className="font-medium text-primary">{inv.studentName}</Link><span>· {inv.studentCode}</span><StatusChip map={INVOICE_STATUS} value={invoiceStatusKey(inv)} /></span>}
        actions={inv?.status === "UNPAID" && <Button variant="secondary" icon={<XCircle className="size-4" />} onClick={() => setCancelOpen(true)}>Hủy hóa đơn</Button>}
      />
      {!inv ? <Card><ListSkeleton /></Card> : (
        <div className="grid gap-5 lg:grid-cols-5">
          <div className="space-y-5 lg:col-span-3">
            <Card>
              <CardHeader title={inv.description} description={`Lập ngày ${formatDateTime(inv.createdAt)}${inv.classCode ? ` · lớp ${inv.classCode}` : ""}`} />
              <CardBody>
                <dl className="tabular divide-y divide-line text-sm">
                  <div className="flex justify-between py-2"><dt className="text-ink-2">Học phí</dt><dd>{formatMoney(inv.subtotal)}</dd></div>
                  {inv.discount > 0 && <div className="flex justify-between py-2"><dt className="text-ink-2">Giảm giá</dt><dd>−{formatMoney(inv.discount)}</dd></div>}
                  <div className="flex justify-between py-2 font-semibold"><dt>Phải thu</dt><dd>{formatMoney(inv.total)}</dd></div>
                  <div className="flex justify-between py-2"><dt className="text-ink-2">Đã thu</dt><dd className="text-ok">{formatMoney(inv.paidAmount)}</dd></div>
                  <div className="flex justify-between py-2 text-base font-bold"><dt>Còn lại</dt><dd className={inv.overdue ? "text-danger" : ""}>{formatMoney(inv.remaining)}</dd></div>
                  <div className="flex justify-between py-2"><dt className="text-ink-2">Hạn thanh toán</dt><dd className={inv.overdue ? "font-medium text-danger" : ""}>{formatDate(inv.dueDate)}{inv.overdue && " · quá hạn"}</dd></div>
                </dl>
                {inv.note && <p className="mt-3 rounded-lg bg-surface-2 px-3 py-2 text-sm text-ink-2">{inv.note}</p>}
              </CardBody>
            </Card>
            <Card>
              <CardHeader title="Lịch sử thanh toán" description={inv.payments?.length ? `${inv.payments.length} lần thu` : undefined} />
              {!inv.payments?.length ? <EmptyState title="Chưa có thanh toán" /> : (
                <ul className="divide-y divide-line border-t border-line">
                  {inv.payments.map((p) => (
                    <li key={p.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
                      <div className="min-w-0 flex-1">
                        <p className="tabular font-semibold">{formatMoney(p.amount)}</p>
                        <p className="tabular truncate text-[12.5px] text-ink-2">{p.receiptNo} · {PAYMENT_METHOD[p.method]} · {formatDateTime(p.paidAt)}{p.receivedByName && ` · ${p.receivedByName}`}</p>
                      </div>
                      <Button size="sm" variant="secondary" icon={<FileText className="size-4" />}
                        onClick={() => download(`/payments/${p.id}/receipt.pdf`, `${p.receiptNo}.pdf`, undefined, true).catch((e) => toast.error(e.message))}>Phiếu thu PDF</Button>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
          <div className="space-y-5 lg:col-span-2">
            {open && (
              <Card>
                <CardHeader title="Ghi nhận thanh toán" description="Có thể thu nhiều lần đến khi đủ" />
                <CardBody>
                  <form noValidate onSubmit={handleSubmit((v) => pay.mutate(v))} className="grid gap-4">
                    <Input label="Số tiền (₫)" inputMode="numeric" error={errors.amount?.message} hint={`Tối đa ${formatMoney(inv.remaining)}`} {...register("amount")} />
                    <Select label="Hình thức" {...register("method")}>
                      {(Object.keys(PAYMENT_METHOD) as PaymentMethod[]).map((m) => <option key={m} value={m}>{PAYMENT_METHOD[m]}</option>)}
                    </Select>
                    <Input label="Ghi chú" placeholder="VD: đóng đợt 1" {...register("note")} />
                    <Button type="submit" size="lg" loading={pay.isPending}>Ghi nhận & tạo phiếu thu</Button>
                  </form>
                </CardBody>
              </Card>
            )}
            {open && (
              qr.isLoading ? <Skeleton className="h-80" /> : qr.isError ? (
                <Card><CardBody className="pt-5 text-sm text-ink-2">{qr.error instanceof ApiError ? qr.error.message : "Không tạo được mã QR"} {" "}<Link to="/app/settings" className="font-semibold text-primary">Mở Cài đặt</Link></CardBody></Card>
              ) : qr.data && <QrPanel qr={qr.data} />
            )}
          </div>
        </div>
      )}
      <ConfirmDialog open={cancelOpen} title="Hủy hóa đơn này?" description="Hóa đơn bị hủy sẽ không tính vào công nợ và không thể thu tiền." confirmLabel="Hủy hóa đơn" danger loading={cancel.isPending}
        onConfirm={() => cancel.mutate()} onCancel={() => setCancelOpen(false)} />
    </>
  );
}
