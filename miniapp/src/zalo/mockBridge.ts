import { getMockSettings } from "./mockState";
import { requestMockCheckout } from "./mockCheckout";
import { ZaloError, type ZaloBridge } from "./types";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
const delay = () => sleep(getMockSettings().latency);

function pickFiles(max: number): Promise<File[]> {
  return new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/jpeg,image/png,image/webp";
    input.multiple = max > 1;
    input.style.display = "none";
    input.onchange = () => {
      resolve(Array.from(input.files ?? []).slice(0, max));
      input.remove();
    };
    document.body.appendChild(input);
    input.click();
  });
}

/** Giả lập zmp-sdk – chạy hoàn toàn trên trình duyệt, khớp quy ước token của MockZaloGraphClient (backend). */
export const mockBridge: ZaloBridge = {
  mode: "mock",
  async getAccessToken() {
    await delay();
    const s = getMockSettings();
    return `mock-${s.zaloId}|${s.name}`;
  },
  async getUserInfo() {
    await delay();
    const s = getMockSettings();
    if (s.denyUserInfo) throw new ZaloError(-1401, "Người dùng từ chối cung cấp thông tin", true);
    return { id: s.zaloId, name: s.name };
  },
  async getPhoneNumberToken() {
    await delay();
    const s = getMockSettings();
    if (s.denyPhone) throw new ZaloError(-201, "Người dùng từ chối chia sẻ số điện thoại", true);
    return `mock-phone-${s.phone}`;
  },
  async getLocationToken() {
    await delay();
    const s = getMockSettings();
    if (s.denyLocation) throw new ZaloError(-201, "Người dùng từ chối chia sẻ vị trí", true);
    return `mock-location-${s.lat},${s.lng}`;
  },
  chooseImages: pickFiles,
  async createOrder(params) {
    const result = await requestMockCheckout(params);
    if (result === "cancel") throw new ZaloError(-201, "Người dùng huỷ thanh toán", true);
    if (result === "fail") throw new ZaloError(-1, "Thanh toán thất bại");
    return { orderId: `MOCK-${Date.now()}` };
  },
  async openWebview(url) {
    window.open(url, "_blank", "noopener");
  },
};
