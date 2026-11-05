package vn.starter.zma.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OrderDtos {
    private OrderDtos() {}

    public record OrderItemRequest(@NotNull Long productId,
                                   @Min(value = 1, message = "Số lượng tối thiểu 1") @Max(value = 99, message = "Số lượng tối đa 99") int quantity) {}

    public record CreateOrderRequest(@NotEmpty(message = "Giỏ hàng trống") @Size(max = 20) List<@Valid OrderItemRequest> items) {}

    public record OrderItemDto(Long productId, String productName, long unitPrice, int quantity) {}

    public record OrderDto(Long id, String code, long amount, OrderStatus status, String description,
                           String paymentMethod, String providerTransId, Instant paidAt, Instant createdAt,
                           List<OrderItemDto> items, Long userId, String userName) {}

    /** Tham số truyền thẳng vào Payment.createOrder(...) của zmp-sdk. */
    public record CheckoutParams(long amount, String desc, List<Map<String, Object>> item, String extradata,
                                 String method, String mac) {}

    public record CreateOrderResponse(OrderDto order, CheckoutParams checkout, String provider) {}
}
