import { ChevronLeft } from "lucide-react";
import type { ReactNode } from "react";
import { Page, useNavigate } from "zmp-ui";

interface Props {
  title: string;
  back?: boolean;
  children: ReactNode;
  footer?: ReactNode;
  /** Chừa chỗ cho BottomNav */
  withNav?: boolean;
}

/**
 * Khung trang chuẩn. app-config.json đặt actionBarHidden=true nên app tự vẽ header
 * (đồng nhất giữa Zalo thật và chế độ mock trên trình duyệt). Chừa khoảng trống cho nút "..." / "X" của Zalo ở góc phải.
 */
export function AppPage({ title, back = true, children, footer, withNav }: Props) {
  const navigate = useNavigate();
  return (
    <Page className="bg-page" style={{ paddingBottom: withNav ? 80 : footer ? 96 : 16 }}>
      <header className="sticky top-0 z-20 bg-primary text-white" style={{ paddingTop: "env(safe-area-inset-top)" }}>
        <div className="h-12 flex items-center gap-1 pl-2 pr-24">
          {back ? (
            <button type="button" aria-label="Quay lại" onClick={() => navigate(-1)} className="w-10 h-10 flex items-center justify-center bg-transparent border-0 text-white">
              <ChevronLeft size={24} />
            </button>
          ) : <span className="w-2" />}
          <h1 className="text-[17px] font-semibold m-0 truncate">{title}</h1>
        </div>
      </header>
      <div className="px-4 pt-3">{children}</div>
      {footer && <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-line px-4 pt-3 safe-bottom z-10">{footer}</div>}
    </Page>
  );
}
