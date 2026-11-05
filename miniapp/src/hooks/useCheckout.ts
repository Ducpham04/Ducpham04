import { useState } from "react";
import { orderApi } from "../api/endpoints";
import { errorMessage } from "../api/client";
import type { Order } from "../api/types";
import { zalo, ZaloError } from "../zalo/bridge";

export type CheckoutState =
  | { phase: "idle" }
  | { phase: "processing" }
  | { phase: "done"; order: Order }
  | { phase: "cancelled" }
  | { phase: "error"; message: string };

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * Luồng thanh toán:
 * 1) POST /orders → backend tính tiền + ký mac
 * 2) Payment.createOrder(checkout) (Checkout SDK / mock sheet)
 * 3) mock: gọi /mock-pay · thật: poll GET /orders/{id} chờ callback của Zalo cập nhật PAID
 */
export function useCheckout() {
  const [state, setState] = useState<CheckoutState>({ phase: "idle" });

  async function checkout(items: { productId: number; quantity: number }[]) {
    setState({ phase: "processing" });
    let orderId: number | null = null;
    let provider: string = "mock";
    try {
      const res = await orderApi.create(items);
      orderId = res.order.id;
      provider = res.provider;
      await zalo.createOrder(res.checkout);
      let order: Order;
      if (provider === "mock") {
        order = await orderApi.mockPay(orderId, true);
      } else {
        order = res.order;
        for (let i = 0; i < 10 && order.status === "PENDING"; i++) {
          await sleep(2000);
          order = await orderApi.get(orderId);
        }
      }
      setState({ phase: "done", order });
    } catch (e) {
      if (e instanceof ZaloError && e.denied) {
        setState({ phase: "cancelled" });
        return;
      }
      if (orderId && provider === "mock" && e instanceof ZaloError) {
        const order = await orderApi.mockPay(orderId, false).catch(() => null);
        if (order) return setState({ phase: "done", order });
      }
      setState({ phase: "error", message: errorMessage(e) });
    }
  }

  return { state, checkout, reset: () => setState({ phase: "idle" }) };
}
