import { DENIED_CODES, ZaloError, type ZaloBridge } from "./types";

// Import động để bundle mock không phụ thuộc runtime Zalo
const sdk = () => import("zmp-sdk/apis");

function wrap(e: unknown): ZaloError {
  const err = e as { code?: number; message?: string };
  const code = typeof err?.code === "number" ? err.code : -1;
  return new ZaloError(code, err?.message ?? "Lỗi Zalo SDK", DENIED_CODES.includes(code));
}

async function fileFromPath(path: string, i: number): Promise<File> {
  const res = await fetch(path);
  const blob = await res.blob();
  const ext = blob.type === "image/png" ? "png" : blob.type === "image/webp" ? "webp" : "jpg";
  return new File([blob], `image-${Date.now()}-${i}.${ext}`, { type: blob.type || "image/jpeg" });
}

/** Gọi zmp-sdk thật – chỉ hoạt động bên trong Zalo (hoặc Zalo Mini App simulator). */
export const realBridge: ZaloBridge = {
  mode: "real",
  async getAccessToken() {
    try {
      const { getAccessToken } = await sdk();
      return await getAccessToken();
    } catch (e) {
      throw wrap(e);
    }
  },
  async getUserInfo() {
    try {
      const { authorize, getUserInfo } = await sdk();
      // Từ SDK 2.35, cần authorize scope.userInfo trước khi lấy tên/ảnh
      await authorize({ scopes: ["scope.userInfo"] });
      const { userInfo } = await getUserInfo({ autoRequestPermission: true });
      return { id: userInfo.id, name: userInfo.name, avatar: userInfo.avatar };
    } catch (e) {
      throw wrap(e);
    }
  },
  async getPhoneNumberToken() {
    try {
      const { getPhoneNumber } = await sdk();
      const { token } = await getPhoneNumber();
      if (!token) throw new ZaloError(-1, "Không nhận được token số điện thoại");
      return token;
    } catch (e) {
      throw e instanceof ZaloError ? e : wrap(e);
    }
  },
  async getLocationToken() {
    try {
      const { getLocation } = await sdk();
      const { token } = await getLocation();
      if (!token) throw new ZaloError(-1, "Không nhận được token vị trí");
      return token;
    } catch (e) {
      throw e instanceof ZaloError ? e : wrap(e);
    }
  },
  async chooseImages(max) {
    try {
      const { chooseImage } = await sdk();
      const { filePaths } = await chooseImage({ count: max, sourceType: ["album", "camera"] });
      return Promise.all(filePaths.slice(0, max).map(fileFromPath));
    } catch (e) {
      throw wrap(e);
    }
  },
  async createOrder(params) {
    try {
      const { Payment } = await sdk();
      const res = await Payment.createOrder({
        amount: params.amount,
        desc: params.desc,
        item: params.item,
        extradata: params.extradata,
        method: params.method || undefined,
        mac: params.mac,
      });
      return { orderId: res.orderId };
    } catch (e) {
      throw wrap(e);
    }
  },
  async openWebview(url) {
    try {
      const { openWebview } = await sdk();
      await openWebview({ url });
    } catch (e) {
      throw wrap(e);
    }
  },
};
