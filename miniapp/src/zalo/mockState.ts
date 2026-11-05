import { storage } from "../lib/storage";

/** Cấu hình giả lập, chỉnh trong MockPanel (nút "MOCK" nổi trên màn hình). */
export interface MockSettings {
  zaloId: string;
  name: string;
  denyUserInfo: boolean;
  denyPhone: boolean;
  denyLocation: boolean;
  phone: string;
  lat: number;
  lng: number;
  /** Mô phỏng độ trễ mạng của SDK (ms) */
  latency: number;
}

const KEY = "zma.mock.settings";

export const defaultMockSettings: MockSettings = {
  zaloId: "demo-user",
  name: "Nguyễn Văn An",
  denyUserInfo: false,
  denyPhone: false,
  denyLocation: false,
  phone: "84912345678",
  lat: 21.0287797,
  lng: 105.8524247,
  latency: 300,
};

export function getMockSettings(): MockSettings {
  const raw = storage.get(KEY);
  if (!raw) return defaultMockSettings;
  try {
    return { ...defaultMockSettings, ...JSON.parse(raw) };
  } catch {
    return defaultMockSettings;
  }
}

export function saveMockSettings(s: MockSettings) {
  storage.set(KEY, JSON.stringify(s));
}
