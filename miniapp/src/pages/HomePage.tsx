import { useQuery } from "@tanstack/react-query";
import { Clock, MessageSquareWarning, Newspaper, ShoppingBag } from "lucide-react";
import { Page, useNavigate } from "zmp-ui";
import { contentApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { ErrorState, ListSkeleton, Skeleton } from "../components/StateViews";
import { assetUrl, formatDate, formatVnd, initials } from "../lib/format";
import { config } from "../config";

const SHORTCUTS = [
  { label: "Tin tức", icon: Newspaper, to: "/explore" },
  { label: "Sản phẩm", icon: ShoppingBag, to: "/explore?tab=products" },
  { label: "Gửi phản ánh", icon: MessageSquareWarning, to: "/feedback/new" },
  { label: "Lịch sử", icon: Clock, to: "/history" },
];

export default function HomePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const news = useQuery({ queryKey: ["articles", "home"], queryFn: () => contentApi.articles({ size: 3 }) });
  const products = useQuery({ queryKey: ["products", "featured"], queryFn: () => contentApi.products({ featured: true, size: 4 }) });

  return (
    <Page className="bg-page" style={{ paddingBottom: 80 }}>
      <div className="bg-primary text-white px-4 pt-6 pb-16 rounded-b-3xl" style={{ paddingTop: "calc(env(safe-area-inset-top) + 24px)" }}>
        <div className="flex items-center gap-3">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="w-11 h-11 rounded-full border-2 border-white/40" />
          ) : (
            <div className="w-11 h-11 rounded-full bg-white/20 flex items-center justify-center font-semibold">{initials(user?.name)}</div>
          )}
          <div className="flex-1 min-w-0">
            <div className="text-sm opacity-80">Xin chào,</div>
            <div className="text-lg font-semibold truncate" data-testid="greeting">{user?.name}</div>
          </div>
          {config.zaloMode === "mock" && <span className="text-[10px] font-bold bg-white/20 rounded px-2 py-1">DEMO</span>}
        </div>
      </div>

      <div className="px-4 -mt-10 space-y-5">
        <div className="card p-4 grid grid-cols-4 gap-2">
          {SHORTCUTS.map((s) => (
            <button key={s.label} type="button" onClick={() => navigate(s.to)} className="flex flex-col items-center gap-1.5 bg-transparent border-0 p-0">
              <span className="w-12 h-12 rounded-full bg-primary-soft text-primary flex items-center justify-center">
                <s.icon size={22} />
              </span>
              <span className="text-xs text-ink text-center leading-tight">{s.label}</span>
            </button>
          ))}
        </div>

        <section>
          <div className="flex justify-between items-center mb-2">
            <h2 className="text-base font-semibold m-0">Tin mới</h2>
            <button type="button" className="text-sm text-primary bg-transparent border-0" onClick={() => navigate("/explore")}>Xem tất cả</button>
          </div>
          {news.isPending ? <ListSkeleton /> : news.isError ? (
            <div className="card"><ErrorState message={errorMessage(news.error)} onRetry={() => news.refetch()} /></div>
          ) : (
            <div className="space-y-3">
              {news.data.items.map((a) => (
                <button key={a.id} type="button" onClick={() => navigate(`/news/${a.id}`)} className="card w-full p-3 flex gap-3 text-left border-0">
                  <img src={assetUrl(a.thumbnailUrl)} alt="" className="w-20 h-16 rounded-lg object-cover shrink-0 bg-gray-100" loading="lazy" />
                  <div className="min-w-0">
                    <div className="text-sm font-medium line-clamp-2 text-ink">{a.title}</div>
                    <div className="text-xs text-ink-muted mt-1">{a.category} · {formatDate(a.publishedAt)}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>

        <section>
          <div className="flex justify-between items-center mb-2">
            <h2 className="text-base font-semibold m-0">Sản phẩm nổi bật</h2>
            <button type="button" className="text-sm text-primary bg-transparent border-0" onClick={() => navigate("/explore?tab=products")}>Xem tất cả</button>
          </div>
          {products.isPending ? (
            <div className="grid grid-cols-2 gap-3">{[0, 1].map((i) => <Skeleton key={i} className="h-48 rounded-card" />)}</div>
          ) : products.isError ? (
            <div className="card"><ErrorState message={errorMessage(products.error)} onRetry={() => products.refetch()} /></div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              {products.data.items.map((p) => (
                <button key={p.id} type="button" onClick={() => navigate(`/products/${p.id}`)} className="card overflow-hidden text-left border-0 p-0">
                  <img src={assetUrl(p.imageUrl)} alt="" className="w-full aspect-square object-cover bg-gray-100" loading="lazy" />
                  <div className="p-2.5">
                    <div className="text-sm line-clamp-2 text-ink min-h-[40px]">{p.name}</div>
                    <div className="text-primary font-semibold text-sm mt-1">{formatVnd(p.price)}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>
      </div>
    </Page>
  );
}
