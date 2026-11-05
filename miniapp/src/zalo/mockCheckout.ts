import type { CheckoutParams } from "./types";

export type MockCheckoutResult = "success" | "fail" | "cancel";
type Listener = (params: CheckoutParams, done: (r: MockCheckoutResult) => void) => void;

let listener: Listener | null = null;

/** MockCheckoutHost đăng ký để hiển thị sheet thanh toán giả. */
export function onMockCheckout(l: Listener | null) {
  listener = l;
}

export function requestMockCheckout(params: CheckoutParams): Promise<MockCheckoutResult> {
  return new Promise((resolve) => {
    if (!listener) return resolve("success");
    listener(params, resolve);
  });
}
