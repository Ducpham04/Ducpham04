import { CircleCheck } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useParams } from "react-router-dom";
import { Button, Sheet, useNavigate } from "zmp-ui";
import { contentApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { ErrorState, Skeleton } from "../components/StateViews";
import { useCheckout } from "../hooks/useCheckout";
import { assetUrl, formatVnd } from "../lib/format";

export default function ProductDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["product", id], queryFn: () => contentApi.product(id), enabled: Number.isFinite(id) });
  const [qty, setQty] = useState(1);
  const { state, checkout, reset } = useCheckout();
  const p = q.data;

  const onBuy = async () => {
    if (!p) return;
    await checkout([{ productId: p.id, quantity: qty }]);
    void qc.invalidateQueries({ queryKey: ["orders"] });
    void qc.invalidateQueries({ queryKey: ["product", id] });
  };

  const footer = p && (
    <Button fullWidth loading={state.phase === "processing"} disabled={p.stock < 1} onClick={onBuy} data-testid="buy-button">
      {p.stock < 1 ? "Hết hàng" : `Mua ngay – ${formatVnd(p.price * qty)}`}
    </Button>
  );

  return (
    <AppPage title="Chi tiết sản phẩm" footer={footer}>
      {q.isPending ? (
        <div className="space-y-3" data-testid="loading"><Skeleton className="w-full aspect-square" /><Skeleton className="h-6 w-3/4" /></div>
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : p ? (
        <div className="card overflow-hidden">
          <img src={assetUrl(p.imageUrl)} alt={p.name} className="w-full aspect-square object-cover bg-gray-100" />
          <div className="p-4">
            <h1 className="text-lg font-semibold m-0">{p.name}</h1>
            <div className="text-primary text-xl font-bold mt-1">{formatVnd(p.price)}</div>
            <div className="text-xs text-ink-muted mt-1">Còn {p.stock} sản phẩm</div>
            <div className="flex items-center gap-3 mt-4">
              <span className="text-sm">Số lượng</span>
              <div className="flex items-center border border-line rounded-lg">
                <button type="button" aria-label="Giảm" className="w-9 h-9 bg-transparent border-0" onClick={() => setQty((v) => Math.max(1, v - 1))}>−</button>
                <span className="w-8 text-center" data-testid="qty">{qty}</span>
                <button type="button" aria-label="Tăng" className="w-9 h-9 bg-transparent border-0" onClick={() => setQty((v) => Math.min(Math.min(99, p.stock), v + 1))}>+</button>
              </div>
            </div>
            <p className="text-sm leading-6 text-ink whitespace-pre-line">{p.description}</p>
          </div>
        </div>
      ) : null}

      <Sheet visible={state.phase === "done" || state.phase === "error" || state.phase === "cancelled"} onClose={reset} autoHeight mask handler>
        <div className="p-6 text-center safe-bottom" data-testid="checkout-result">
          {state.phase === "done" && state.order.status === "PAID" ? (
            <>
              <div className="w-16 h-16 mx-auto rounded-full bg-green-50 text-success flex items-center justify-center"><CircleCheck size={40} /></div>
              <div className="text-lg font-semibold mt-3">Thanh toán thành công</div>
              <div className="text-sm text-ink-muted mt-1">Mã đơn {state.order.code} · {formatVnd(state.order.amount)}</div>
              <Button fullWidth className="mt-5" onClick={() => { reset(); navigate(`/orders/${state.order.id}`); }}>Xem đơn hàng</Button>
            </>
          ) : state.phase === "done" ? (
            <>
              <div className="text-lg font-semibold">{state.order.status === "PENDING" ? "Đang chờ xác nhận thanh toán" : "Thanh toán không thành công"}</div>
              <div className="text-sm text-ink-muted mt-1">Mã đơn {state.order.code}</div>
              <Button fullWidth variant="secondary" className="mt-5" onClick={() => { reset(); navigate(`/orders/${state.order.id}`); }}>Xem đơn hàng</Button>
            </>
          ) : state.phase === "cancelled" ? (
            <>
              <div className="text-lg font-semibold">Bạn đã huỷ thanh toán</div>
              <Button fullWidth variant="secondary" className="mt-5" onClick={reset}>Đóng</Button>
            </>
          ) : state.phase === "error" ? (
            <>
              <div className="text-lg font-semibold text-danger">Không thể thanh toán</div>
              <div className="text-sm text-ink-muted mt-1">{state.message}</div>
              <Button fullWidth variant="secondary" className="mt-5" onClick={reset}>Đóng</Button>
            </>
          ) : null}
        </div>
      </Sheet>
    </AppPage>
  );
}
