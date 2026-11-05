package vn.starter.zma.order;

import vn.starter.zma.order.OrderDtos.CheckoutParams;

/**
 * Nhà cung cấp thanh toán. {@code app.payment.provider}:
 * <ul>
 *   <li>{@code mock}: Mini App hiển thị sheet thanh toán giả, gọi /orders/{id}/mock-pay</li>
 *   <li>{@code zalo}: Zalo Checkout SDK thật (Payment.createOrder), xác nhận qua callback /payments/zalo/notify</li>
 * </ul>
 */
public interface PaymentProvider {
    String name();

    CheckoutParams createCheckout(Order order);
}
