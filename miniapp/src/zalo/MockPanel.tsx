import { useEffect, useState } from "react";
import { Button, Sheet } from "zmp-ui";
import { formatVnd } from "../lib/format";
import { onMockCheckout, type MockCheckoutResult } from "./mockCheckout";
import { defaultMockSettings, getMockSettings, saveMockSettings, type MockSettings } from "./mockState";
import type { CheckoutParams } from "./types";

/** Nút "MOCK" nổi + bảng chỉnh hành vi giả lập. Chỉ render khi config.zaloMode === "mock". */
export function MockPanel() {
  const [open, setOpen] = useState(false);
  const [s, setS] = useState<MockSettings>(getMockSettings());
  const set = <K extends keyof MockSettings>(k: K, v: MockSettings[K]) => setS((p) => ({ ...p, [k]: v }));

  const apply = (reload: boolean) => {
    saveMockSettings(s);
    setOpen(false);
    if (reload) {
      localStorage.removeItem("zma.accessToken");
      localStorage.removeItem("zma.refreshToken");
      window.location.reload();
    }
  };

  return (
    <>
      <button type="button" onClick={() => setOpen(true)} data-testid="mock-panel-toggle"
        className="fixed z-[9999] right-3 bottom-24 rounded-full bg-amber-400 text-black text-[11px] font-bold px-3 py-2 shadow-sheet border-0">
        MOCK
      </button>
      <Sheet visible={open} onClose={() => setOpen(false)} autoHeight title="Giả lập Zalo (chế độ Mock)">
        <div className="p-4 space-y-3 text-sm safe-bottom">
          <p className="text-ink-muted m-0">Ứng dụng đang chạy ngoài Zalo. Các API zmp-sdk được giả lập để demo.</p>
          <label className="block">Zalo ID
            <input className="w-full border border-line rounded-lg px-3 py-2 mt-1" value={s.zaloId} onChange={(e) => set("zaloId", e.target.value.replace(/[^A-Za-z0-9_-]/g, ""))} />
          </label>
          <label className="block">Tên Zalo
            <input className="w-full border border-line rounded-lg px-3 py-2 mt-1" value={s.name} onChange={(e) => set("name", e.target.value)} />
          </label>
          {([
            ["denyUserInfo", "Từ chối quyền tên & ảnh"],
            ["denyPhone", "Từ chối quyền số điện thoại"],
            ["denyLocation", "Từ chối quyền vị trí"],
          ] as const).map(([k, label]) => (
            <label key={k} className="flex items-center justify-between">
              <span>{label}</span>
              <input type="checkbox" checked={s[k]} onChange={(e) => set(k, e.target.checked)} data-testid={`mock-${k}`} />
            </label>
          ))}
          <div className="flex gap-2 pt-2">
            <Button size="small" variant="secondary" onClick={() => setS(defaultMockSettings)}>Mặc định</Button>
            <Button size="small" variant="secondary" onClick={() => apply(true)}>Lưu & đăng nhập lại</Button>
            <Button size="small" onClick={() => apply(false)} data-testid="mock-save">Lưu</Button>
          </div>
        </div>
      </Sheet>
    </>
  );
}

/** Sheet giả lập Checkout SDK. */
export function MockCheckoutHost() {
  const [req, setReq] = useState<{ params: CheckoutParams; done: (r: MockCheckoutResult) => void } | null>(null);
  useEffect(() => {
    onMockCheckout((params, done) => setReq({ params, done }));
    return () => onMockCheckout(null);
  }, []);
  const finish = (r: MockCheckoutResult) => {
    req?.done(r);
    setReq(null);
  };
  return (
    <Sheet visible={!!req} onClose={() => finish("cancel")} autoHeight title="Thanh toán (giả lập)">
      {req && (
        <div className="p-4 space-y-3 safe-bottom" data-testid="mock-checkout">
          <div className="text-center">
            <div className="text-sm text-ink-muted">{req.params.desc}</div>
            <div className="text-2xl font-bold text-primary mt-1">{formatVnd(req.params.amount)}</div>
            <div className="text-[11px] text-ink-muted mt-1 break-all">mac: {req.params.mac.slice(0, 24)}…</div>
          </div>
          <Button fullWidth onClick={() => finish("success")} data-testid="mock-pay-success">Thanh toán thành công</Button>
          <Button fullWidth variant="secondary" onClick={() => finish("fail")}>Mô phỏng thất bại</Button>
          <Button fullWidth variant="tertiary" onClick={() => finish("cancel")}>Huỷ</Button>
        </div>
      )}
    </Sheet>
  );
}
