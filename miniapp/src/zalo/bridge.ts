import { config } from "../config";
import { mockBridge } from "./mockBridge";
import { realBridge } from "./realBridge";
import type { ZaloBridge } from "./types";

export const zalo: ZaloBridge = config.zaloMode === "mock" ? mockBridge : realBridge;
export { ZaloError } from "./types";
