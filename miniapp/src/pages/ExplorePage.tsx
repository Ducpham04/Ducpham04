import { Search } from "lucide-react";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Button, useNavigate } from "zmp-ui";
import { contentApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { AppPage } from "../components/AppPage";
import { EmptyState, ErrorState, ListSkeleton } from "../components/StateViews";
import { assetUrl, formatDate, formatVnd } from "../lib/format";

function useDebounced<T>(value: T, ms = 400) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export default function ExplorePage() {
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") === "products" ? "products" : "news";
  const [q, setQ] = useState("");
  const query = useDebounced(q);

  return (
    <AppPage title="Khám phá" back={false} withNav>
      <div className="flex bg-white rounded-lg p-1 mb-3 shadow-card">
        {(["news", "products"] as const).map((t) => (
          <button key={t} type="button" onClick={() => setParams(t === "products" ? { tab: t } : {}, { replace: true })}
            className={`flex-1 py-2 rounded-md text-sm font-medium border-0 ${tab === t ? "bg-primary text-white" : "bg-transparent text-ink-muted"}`}>
            {t === "news" ? "Tin tức" : "Sản phẩm"}
          </button>
        ))}
      </div>
      <label className="flex items-center gap-2 bg-white rounded-lg px-3 h-10 mb-3 shadow-card">
        <Search size={20} className="text-ink-muted" />
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder={tab === "news" ? "Tìm tin tức…" : "Tìm sản phẩm…"}
          className="flex-1 border-0 outline-none text-sm bg-transparent" aria-label="Tìm kiếm" />
      </label>
      {tab === "news" ? <NewsList q={query} /> : <ProductList q={query} />}
    </AppPage>
  );
}

function NewsList({ q }: { q: string }) {
  const navigate = useNavigate();
  const [category, setCategory] = useState<string>();
  const cats = useQuery({ queryKey: ["article-categories"], queryFn: contentApi.categories });
  const list = useInfiniteQuery({
    queryKey: ["articles", q, category],
    queryFn: ({ pageParam }) => contentApi.articles({ q, category, page: pageParam, size: 10 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const items = list.data?.pages.flatMap((p) => p.items) ?? [];

  return (
    <div>
      <div className="flex gap-2 overflow-x-auto pb-3 -mx-4 px-4">
        {[undefined, ...(cats.data ?? [])].map((c) => (
          <button key={c ?? "all"} type="button" onClick={() => setCategory(c)}
            className={`whitespace-nowrap px-3 py-1.5 rounded-full text-sm border ${category === c ? "bg-primary text-white border-primary" : "bg-white text-ink border-line"}`}>
            {c ?? "Tất cả"}
          </button>
        ))}
      </div>
      {list.isPending ? <ListSkeleton rows={4} /> : list.isError ? (
        <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState title="Chưa có tin nào" description="Thử đổi từ khoá hoặc chuyên mục khác." />
      ) : (
        <div className="space-y-3">
          {items.map((a) => (
            <button key={a.id} type="button" onClick={() => navigate(`/news/${a.id}`)} className="card w-full p-3 flex gap-3 text-left border-0" data-testid="article-item">
              <img src={assetUrl(a.thumbnailUrl)} alt="" className="w-24 h-20 rounded-lg object-cover shrink-0 bg-gray-100" loading="lazy" />
              <div className="min-w-0">
                <div className="text-sm font-medium line-clamp-2 text-ink">{a.title}</div>
                <div className="text-xs text-ink-muted mt-1 line-clamp-2">{a.summary}</div>
                <div className="text-xs text-ink-muted mt-1">{formatDate(a.publishedAt)}</div>
              </div>
            </button>
          ))}
          {list.hasNextPage && (
            <Button variant="secondary" fullWidth loading={list.isFetchingNextPage} onClick={() => list.fetchNextPage()}>Xem thêm</Button>
          )}
        </div>
      )}
    </div>
  );
}

function ProductList({ q }: { q: string }) {
  const navigate = useNavigate();
  const list = useInfiniteQuery({
    queryKey: ["products", q],
    queryFn: ({ pageParam }) => contentApi.products({ q, page: pageParam, size: 10 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const items = list.data?.pages.flatMap((p) => p.items) ?? [];
  if (list.isPending) return <ListSkeleton rows={4} />;
  if (list.isError) return <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} />;
  if (items.length === 0) return <EmptyState title="Không tìm thấy sản phẩm" />;
  return (
    <div>
      <div className="grid grid-cols-2 gap-3">
        {items.map((p) => (
          <button key={p.id} type="button" onClick={() => navigate(`/products/${p.id}`)} className="card overflow-hidden text-left border-0 p-0" data-testid="product-item">
            <img src={assetUrl(p.imageUrl)} alt="" className="w-full aspect-square object-cover bg-gray-100" loading="lazy" />
            <div className="p-2.5">
              <div className="text-sm line-clamp-2 text-ink min-h-[40px]">{p.name}</div>
              <div className="text-primary font-semibold text-sm mt-1">{formatVnd(p.price)}</div>
            </div>
          </button>
        ))}
      </div>
      {list.hasNextPage && (
        <Button className="mt-3" variant="secondary" fullWidth loading={list.isFetchingNextPage} onClick={() => list.fetchNextPage()}>Xem thêm</Button>
      )}
    </div>
  );
}
