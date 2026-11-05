import { useInfiniteQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Button, useNavigate } from "zmp-ui";
import { feedbackApi, orderApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { EmptyState, ErrorState, ListSkeleton } from "../components/StateViews";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatVnd } from "../lib/format";

export default function HistoryPage() {
  const [tab, setTab] = useState<"feedback" | "orders">("feedback");
  return (
    <AppPage title="Lịch sử của tôi" back={false} withNav>
      <div className="flex bg-white rounded-lg p-1 mb-3 shadow-card">
        {(["feedback", "orders"] as const).map((t) => (
          <button key={t} type="button" onClick={() => setTab(t)}
            className={`flex-1 py-2 rounded-md text-sm font-medium border-0 ${tab === t ? "bg-primary text-white" : "bg-transparent text-ink-muted"}`}>
            {t === "feedback" ? "Phản ánh" : "Đơn hàng"}
          </button>
        ))}
      </div>
      {tab === "feedback" ? <FeedbackHistory /> : <OrderHistory />}
    </AppPage>
  );
}

function usePaged<T>(key: string, fn: (page: number) => Promise<{ items: T[]; page: number; totalPages: number }>) {
  return useInfiniteQuery({
    queryKey: [key, "mine"],
    queryFn: ({ pageParam }) => fn(pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
}

function FeedbackHistory() {
  const navigate = useNavigate();
  const q = usePaged("feedbacks", (p) => feedbackApi.mine(p));
  const items = q.data?.pages.flatMap((p) => p.items) ?? [];
  if (q.isPending) return <ListSkeleton />;
  if (q.isError) return <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />;
  if (!items.length) return <EmptyState title="Bạn chưa gửi phản ánh nào" action={{ label: "Gửi phản ánh đầu tiên", onClick: () => navigate("/feedback/new") }} />;
  return (
    <div className="space-y-3">
      {items.map((f) => (
        <button key={f.id} type="button" onClick={() => navigate(`/feedback/${f.id}`)} className="card w-full p-3 text-left border-0" data-testid="feedback-item">
          <div className="flex justify-between items-center gap-2">
            <span className="text-xs text-ink-muted">{f.code}</span>
            <StatusBadge status={f.status} />
          </div>
          <div className="text-sm font-medium mt-1 line-clamp-2 text-ink">{f.title}</div>
          <div className="text-xs text-ink-muted mt-1">{f.category} · {formatDate(f.createdAt)}</div>
        </button>
      ))}
      {q.hasNextPage && <Button variant="secondary" fullWidth loading={q.isFetchingNextPage} onClick={() => q.fetchNextPage()}>Xem thêm</Button>}
    </div>
  );
}

function OrderHistory() {
  const navigate = useNavigate();
  const q = usePaged("orders", (p) => orderApi.mine(p));
  const items = q.data?.pages.flatMap((p) => p.items) ?? [];
  if (q.isPending) return <ListSkeleton />;
  if (q.isError) return <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />;
  if (!items.length) return <EmptyState title="Chưa có đơn hàng" action={{ label: "Xem sản phẩm", onClick: () => navigate("/explore?tab=products") }} />;
  return (
    <div className="space-y-3">
      {items.map((o) => (
        <button key={o.id} type="button" onClick={() => navigate(`/orders/${o.id}`)} className="card w-full p-3 text-left border-0" data-testid="order-item">
          <div className="flex justify-between items-center">
            <span className="text-xs text-ink-muted">{o.code}</span>
            <StatusBadge status={o.status} />
          </div>
          <div className="text-sm font-medium mt-1 line-clamp-2 text-ink">{o.description}</div>
          <div className="flex justify-between text-xs mt-1">
            <span className="text-ink-muted">{formatDate(o.createdAt, true)}</span>
            <span className="text-primary font-semibold text-sm">{formatVnd(o.amount)}</span>
          </div>
        </button>
      ))}
      {q.hasNextPage && <Button variant="secondary" fullWidth loading={q.isFetchingNextPage} onClick={() => q.fetchNextPage()}>Xem thêm</Button>}
    </div>
  );
}
