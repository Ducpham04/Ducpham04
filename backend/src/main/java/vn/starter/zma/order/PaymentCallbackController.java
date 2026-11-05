package vn.starter.zma.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Payment callback")
@RestController
@RequestMapping("/api/v1/payments/zalo")
@RequiredArgsConstructor
public class PaymentCallbackController {
    private final OrderService service;

    /** Cấu hình URL này ở mục Thanh toán (Checkout) của Mini App trên Zalo Developers. */
    @Operation(summary = "Callback (notify) từ Zalo Checkout SDK")
    @PostMapping("/notify")
    public Map<String, Object> notify(@RequestBody NotifyBody body) {
        boolean ok = service.handleZaloNotify(body.data(), body.mac());
        return ok ? Map.of("returnCode", 1, "returnMessage", "success")
                : Map.of("returnCode", 0, "returnMessage", "invalid");
    }

    public record NotifyBody(Map<String, Object> data, String mac) {}
}
