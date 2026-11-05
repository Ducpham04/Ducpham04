import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { orderApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { ErrorState, Skeleton } from "../components/StateViews";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatVnd } from "../lib/format";

export default function OrderDetailPage() {
  const id = Number(useParams().id);
  const q = useQuery({ queryKey: ["orders", id], queryFn: () => orderApi.get(id), enabled: Number.isFinite(id) });
  const o = q.data;
  return (
    <AppPage title="Chi tiết đơn hàng">
      {q.isPending ? <Skeleton className="h-48" /> : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : o ? (
        <div className="card p-4 space-y-3">
          <div className="flex justify-between items-center">
            <span className="font-semibold" data-testid="order-code">{o.code}</span>
            <StatusBadge status={o.status} />
          </div>
          <div className="text-xs text-ink-muted">Tạo lúc {formatDate(o.createdAt, true)}{o.paidAt && ` · Thanh toán ${formatDate(o.paidAt, true)}`}</div>
          <div className="divide-y divide-line border-t border-b border-line">
            {o.items?.map((i) => (
              <div key={i.productId} className="flex justify-between py-2 text-sm">
                <span>{i.productName} × {i.quantity}</span>
                <span>{formatVnd(i.unitPrice * i.quantity)}</span>
              </div>
            ))}
          </div>
          <div className="flex justify-between font-semibold">
            <span>Tổng cộng</span>
            <span className="text-primary">{formatVnd(o.amount)}</span>
          </div>
          {o.paymentMethod && <div className="text-xs text-ink-muted">Phương thức: {o.paymentMethod}</div>}
        </div>
      ) : null}
    </AppPage>
  );
}
