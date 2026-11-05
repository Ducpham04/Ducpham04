import { CircleUser, Info, MapPin, Pencil, Phone } from "lucide-react";
import { useState } from "react";
import { Button, Sheet, useSnackbar } from "zmp-ui";
import type { LucideIcon } from "lucide-react";
import { meApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { AppPage } from "../components/AppPage";
import { PermissionPrompt } from "../components/PermissionPrompt";
import { config } from "../config";
import { initials } from "../lib/format";
import { zalo, ZaloError } from "../zalo/bridge";

export default function ProfilePage() {
  const { user, setUser, logout } = useAuth();
  const { openSnackbar } = useSnackbar();
  const [phoneState, setPhoneState] = useState<"idle" | "loading" | "denied">("idle");
  const [infoDenied, setInfoDenied] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [name, setName] = useState(user?.name ?? "");
  const [saving, setSaving] = useState(false);

  const linkPhone = async () => {
    setPhoneState("loading");
    try {
      // Token chỉ dùng được 1 lần, hết hạn sau 2 phút → gửi ngay cho backend giải mã
      const [token, zaloToken] = await Promise.all([zalo.getPhoneNumberToken(), zalo.getAccessToken()]);
      setUser(await meApi.linkPhone(token, zaloToken));
      openSnackbar({ text: "Đã liên kết số điện thoại", type: "success" });
      setPhoneState("idle");
    } catch (e) {
      if (e instanceof ZaloError && e.denied) return setPhoneState("denied");
      setPhoneState("idle");
      openSnackbar({ text: errorMessage(e), type: "error" });
    }
  };

  const syncFromZalo = async () => {
    try {
      const info = await zalo.getUserInfo();
      setUser(await meApi.update(info.name));
      setInfoDenied(false);
      openSnackbar({ text: "Đã cập nhật từ Zalo", type: "success" });
    } catch (e) {
      if (e instanceof ZaloError && e.denied) return setInfoDenied(true);
      openSnackbar({ text: errorMessage(e), type: "error" });
    }
  };

  const saveName = async () => {
    if (name.trim().length < 2) return openSnackbar({ text: "Tên từ 2–150 ký tự", type: "error" });
    setSaving(true);
    try {
      setUser(await meApi.update(name.trim()));
      setEditOpen(false);
    } catch (e) {
      openSnackbar({ text: errorMessage(e), type: "error" });
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppPage title="Hồ sơ" back={false} withNav>
      <div className="card p-4 flex items-center gap-3">
        {user?.avatarUrl ? <img src={user.avatarUrl} alt="" className="w-16 h-16 rounded-full" /> : (
          <div className="w-16 h-16 rounded-full bg-primary-soft text-primary flex items-center justify-center text-xl font-semibold">{initials(user?.name)}</div>
        )}
        <div className="flex-1 min-w-0">
          <div className="text-lg font-semibold truncate" data-testid="profile-name">{user?.name}</div>
          <div className="text-xs text-ink-muted">ID Zalo: {user?.zaloId}</div>
        </div>
        <button type="button" aria-label="Sửa tên" className="bg-transparent border-0 text-primary" onClick={() => { setName(user?.name ?? ""); setEditOpen(true); }}>
          <Pencil size={20} />
        </button>
      </div>

      <div className="card mt-3 divide-y divide-line">
        <Row icon={Phone} label="Số điện thoại">
          {user?.phone ? <span className="text-sm" data-testid="profile-phone">{user.phone}</span> : (
            <Button size="small" loading={phoneState === "loading"} onClick={linkPhone} data-testid="link-phone">Liên kết</Button>
          )}
        </Row>
        <Row icon={CircleUser} label="Tên & ảnh từ Zalo">
          <Button size="small" variant="tertiary" onClick={syncFromZalo}>Đồng bộ</Button>
        </Row>
        <Row icon={MapPin} label="Vị trí gần nhất">
          <span className="text-sm text-ink-muted">{user?.lastLat != null ? `${Number(user.lastLat).toFixed(4)}, ${Number(user.lastLng).toFixed(4)}` : "Chưa có"}</span>
        </Row>
        <Row icon={Info} label="Phiên bản">
          <span className="text-sm text-ink-muted">{config.version} · {config.zaloMode === "mock" ? "Chế độ DEMO" : "Zalo"}</span>
        </Row>
      </div>

      <div className="mt-3 space-y-3">
        {phoneState === "denied" && (
          <PermissionPrompt title="Bạn chưa chia sẻ số điện thoại"
            reason="Số điện thoại giúp cán bộ liên hệ lại khi xử lý phản ánh và gửi thông báo kết quả qua Zalo."
            onRetry={linkPhone} onDismiss={() => setPhoneState("idle")} />
        )}
        {infoDenied && (
          <PermissionPrompt title="Bạn chưa cho phép truy cập tên & ảnh đại diện"
            reason="Bạn vẫn có thể tự đặt tên hiển thị bằng nút chỉnh sửa." onRetry={syncFromZalo} onDismiss={() => setInfoDenied(false)} />
        )}
        <Button fullWidth variant="secondary" onClick={() => void logout()}>Đăng xuất</Button>
      </div>

      <Sheet visible={editOpen} onClose={() => setEditOpen(false)} autoHeight title="Đổi tên hiển thị">
        <div className="p-4 safe-bottom space-y-3">
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={150} aria-label="Tên hiển thị"
            className="w-full rounded-lg border border-line px-3 py-2.5 text-[15px] outline-none focus:border-primary" />
          <Button fullWidth loading={saving} onClick={saveName}>Lưu</Button>
        </div>
      </Sheet>
    </AppPage>
  );
}

function Row({ icon: Icon, label, children }: { icon: LucideIcon; label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3 px-4 py-3 min-h-[56px]">
      <Icon size={20} className="text-primary" />
      <span className="flex-1 text-sm">{label}</span>
      {children}
    </div>
  );
}
