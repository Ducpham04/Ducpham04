import { MapPin } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { feedbackApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import type { FeedbackStatus } from "../api/types";
import { AppPage } from "../components/AppPage";
import { ErrorState, Skeleton } from "../components/StateViews";
import { StatusBadge } from "../components/StatusBadge";
import { assetUrl, formatDate } from "../lib/format";
import { zalo } from "../zalo/bridge";

const STEPS: { key: FeedbackStatus | "DONE"; label: string }[] = [
  { key: "NEW", label: "Đã gửi" },
  { key: "IN_PROGRESS", label: "Đang xử lý" },
  { key: "DONE", label: "Kết quả" },
];

function stepIndex(s: FeedbackStatus) {
  return s === "NEW" ? 0 : s === "IN_PROGRESS" ? 1 : 2;
}

export default function FeedbackDetailPage() {
  const id = Number(useParams().id);
  const q = useQuery({ queryKey: ["feedbacks", id], queryFn: () => feedbackApi.get(id), enabled: Number.isFinite(id) });
  const f = q.data;
  return (
    <AppPage title="Chi tiết phản ánh">
      {q.isPending ? (
        <div className="space-y-3" data-testid="loading"><Skeleton className="h-24" /><Skeleton className="h-40" /></div>
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : f ? (
        <div className="space-y-3 pb-4">
          <div className="card p-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-ink-muted" data-testid="feedback-code">{f.code}</span>
              <StatusBadge status={f.status} />
            </div>
            <h1 className="text-lg font-semibold mt-2 mb-1">{f.title}</h1>
            <div className="text-xs text-ink-muted">{f.category} · {formatDate(f.createdAt, true)}</div>
            <div className="flex items-center mt-4">
              {STEPS.map((s, i) => {
                const active = i <= stepIndex(f.status);
                return (
                  <div key={s.key} className="flex-1 flex flex-col items-center relative">
                    {i > 0 && <div className={`absolute top-3 right-1/2 w-full h-0.5 ${active ? "bg-primary" : "bg-line"}`} />}
                    <div className={`relative z-[1] w-6 h-6 rounded-full flex items-center justify-center text-xs text-white ${active ? (f.status === "REJECTED" && i === 2 ? "bg-danger" : "bg-primary") : "bg-gray-300"}`}>{i + 1}</div>
                    <div className="text-[11px] mt-1 text-ink-muted">{i === 2 && f.status === "REJECTED" ? "Từ chối" : s.label}</div>
                  </div>
                );
              })}
            </div>
          </div>
          <div className="card p-4">
            <div className="text-sm whitespace-pre-line leading-6">{f.content}</div>
            {f.address && <div className="text-sm text-ink-muted mt-3 flex gap-1"><MapPin size={18} /> {f.address}</div>}
            {f.lat != null && f.lng != null && (
              <button type="button" className="text-sm text-primary bg-transparent border-0 p-0 mt-2"
                onClick={() => zalo.openWebview(`https://www.google.com/maps?q=${f.lat},${f.lng}`)}>Xem trên bản đồ</button>
            )}
            {f.images.length > 0 && (
              <div className="grid grid-cols-3 gap-2 mt-3">
                {f.images.map((u) => <img key={u} src={assetUrl(u)} alt="Ảnh phản ánh" className="aspect-square object-cover rounded-lg bg-gray-100" />)}
              </div>
            )}
          </div>
          <div className="card p-4">
            <div className="text-sm font-semibold mb-1">Phản hồi từ cơ quan</div>
            <div className="text-sm text-ink-muted whitespace-pre-line" data-testid="admin-reply">{f.adminReply ?? "Chưa có phản hồi. Chúng tôi sẽ thông báo qua Zalo khi có kết quả."}</div>
          </div>
        </div>
      ) : null}
    </AppPage>
  );
}
