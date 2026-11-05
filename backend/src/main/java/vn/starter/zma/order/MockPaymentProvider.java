package vn.starter.zma.order;

import vn.starter.zma.order.OrderDtos.CheckoutParams;

public class MockPaymentProvider implements PaymentProvider {
    private final ZaloCheckoutPaymentProvider delegate;

    public MockPaymentProvider(ZaloCheckoutPaymentProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return "mock";
    }

    /** Vẫn ký mac như thật để dev kiểm tra được định dạng tham số. */
    @Override
    public CheckoutParams createCheckout(Order order) {
        return delegate.createCheckout(order);
    }
}
