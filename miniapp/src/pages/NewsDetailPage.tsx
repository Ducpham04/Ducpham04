import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { contentApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { ErrorState, Skeleton } from "../components/StateViews";
import { assetUrl, formatDate } from "../lib/format";

export default function NewsDetailPage() {
  const id = Number(useParams().id);
  const q = useQuery({ queryKey: ["article", id], queryFn: () => contentApi.article(id), enabled: Number.isFinite(id) });
  return (
    <AppPage title="Tin tức">
      {q.isPending ? (
        <div className="space-y-3" data-testid="loading"><Skeleton className="w-full aspect-video" /><Skeleton className="h-6 w-4/5" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-2/3" /></div>
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : (
        <article className="card overflow-hidden">
          <img src={assetUrl(q.data.thumbnailUrl)} alt="" className="w-full aspect-video object-cover bg-gray-100" />
          <div className="p-4">
            <span className="text-xs font-medium text-primary bg-primary-soft px-2 py-0.5 rounded-full">{q.data.category}</span>
            <h1 className="text-xl font-semibold mt-2 mb-1 leading-7">{q.data.title}</h1>
            <div className="text-xs text-ink-muted mb-3">{formatDate(q.data.publishedAt, true)}</div>
            {q.data.summary && <p className="font-medium text-ink mt-0">{q.data.summary}</p>}
            <div className="text-[15px] leading-6 whitespace-pre-line text-ink">{q.data.content}</div>
          </div>
        </article>
      )}
    </AppPage>
  );
}
