package vn.starter.zma.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.order.OrderDtos.*;
import vn.starter.zma.security.CurrentUser;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @Operation(summary = "Tạo đơn và nhận tham số cho Payment.createOrder (Checkout SDK)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return service.create(CurrentUser.get().id(), req);
    }

    @GetMapping("/me")
    public PageResponse<OrderDto> mine(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return service.mine(CurrentUser.get().id(), page, size);
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable Long id) {
        return service.get(id, CurrentUser.get());
    }

    @Operation(summary = "[Chỉ chế độ mock] Giả lập kết quả thanh toán")
    @PostMapping("/{id}/mock-pay")
    public OrderDto mockPay(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean success) {
        return service.mockPay(id, CurrentUser.get(), success);
    }
}
