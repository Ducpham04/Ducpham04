/** Lỗi chuẩn hoá từ zmp-sdk / mock. */
export class ZaloError extends Error {
  constructor(public code: number | string, message: string, public denied = false) {
    super(message);
    this.name = "ZaloError";
  }
}

/** Mã lỗi zmp-sdk khi người dùng từ chối quyền (-201: authorize/phone/location, -1401: userInfo, -2002: đóng popup). */
export const DENIED_CODES = [-201, -1401, -2002, -203];

export interface ZaloUserInfo {
  id: string;
  name: string;
  avatar?: string;
}

export interface CheckoutParams {
  amount: number;
  desc: string;
  item: Record<string, unknown>[];
  extradata?: string;
  method?: string;
  mac: string;
}

/**
 * Lớp adapter cho mọi API Zalo mà app dùng.
 * Code UI KHÔNG import trực tiếp `zmp-sdk` – luôn đi qua đây để chạy được ở chế độ mock.
 */
export interface ZaloBridge {
  readonly mode: "mock" | "real";
  getAccessToken(): Promise<string>;
  /** Xin quyền + lấy tên/ảnh đại diện. Ném ZaloError(denied=true) nếu từ chối. */
  getUserInfo(): Promise<ZaloUserInfo>;
  /** Trả về token để backend giải mã số điện thoại. */
  getPhoneNumberToken(): Promise<string>;
  /** Trả về token để backend giải mã vị trí. */
  getLocationToken(): Promise<string>;
  /** Chọn ảnh từ thư viện/camera. */
  chooseImages(max: number): Promise<File[]>;
  /** Mở Checkout SDK. Trả về orderId của Zalo. */
  createOrder(params: CheckoutParams): Promise<{ orderId: string }>;
  openWebview(url: string): Promise<void>;
}
