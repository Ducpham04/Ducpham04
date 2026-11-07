package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.config.AppProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "Webhook thanh toán")
@RestController
@RequiredArgsConstructor
public class WebhookController {
    private final List<PaymentWebhookParser> parsers;
    private final PaymentService paymentService;
    private final BookingRepository bookings;
    private final AppProperties props;

    private Map<String, PaymentWebhookParser> byProvider() {
        return parsers.stream().collect(Collectors.toMap(PaymentWebhookParser::provider, Function.identity()));
    }

    @PostMapping("/api/webhooks/payments/{provider}")
    public Map<String, Object> receive(@PathVariable String provider, @RequestBody JsonNode payload,
                                       @RequestHeader Map<String, String> headers) {
        if (!secretMatches(headers)) throw ApiException.unauthorized("Webhook secret không hợp lệ");
        PaymentWebhookParser parser = byProvider().get(provider.toLowerCase(Locale.ROOT));
        if (parser == null) throw ApiException.notFound("cổng thanh toán");
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (IncomingTransfer t : parser.parse(payload)) {
            PaymentService.Result r;
            try {
                r = paymentService.apply(parser.provider(), t);
            } catch (DataIntegrityViolationException e) {
                r = PaymentService.Result.DUPLICATE;
            }
            summary.merge(r.name(), 1, Integer::sum);
        }
        return Map.of("success", true, "result", summary);
    }

    /**
     * Giả lập khách đã chuyển khoản – chỉ bật ở môi trường demo (app.payment.mock-enabled=true).
     * Đi qua đúng luồng xử lý webhook để demo end-to-end.
     */
    @PostMapping("/api/public/bookings/{code}/simulate-payment")
    public Map<String, Object> simulate(@PathVariable String code, @RequestParam String phone) {
        if (!props.payment().mockEnabled()) throw ApiException.notFound("chức năng");
        String normalized = TextUtils.normalizePhone(phone);
        Booking bk = bookings.findByCodeDetailed(code.toUpperCase(Locale.ROOT))
                .filter(b -> b.getCustomer().getPhone().equals(normalized))
                .orElseThrow(() -> ApiException.notFound("lịch hẹn"));
        long amount = Math.max(bk.getAmountDue() - bk.getAmountPaid(), 0);
        PaymentService.Result r = paymentService.apply("mock",
                new IncomingTransfer("SIM-" + UUID.randomUUID(), amount, "CK " + bk.getCode()));
        return Map.of("success", true, "result", r.name());
    }

    private boolean secretMatches(Map<String, String> headers) {
        String expected = props.payment().webhookSecret();
        if (expected == null || expected.isBlank()) return false;
        List<String> candidates = new ArrayList<>();
        Map<String, String> h = new HashMap<>();
        headers.forEach((k, v) -> h.put(k.toLowerCase(Locale.ROOT), v));
        candidates.add(h.get("x-webhook-secret"));
        candidates.add(h.get("secure-token"));
        String auth = h.get("authorization");
        if (auth != null) candidates.add(auth.replaceFirst("(?i)^(apikey|bearer)\\s+", ""));
        byte[] exp = expected.getBytes(StandardCharsets.UTF_8);
        return candidates.stream().filter(Objects::nonNull)
                .anyMatch(c -> MessageDigest.isEqual(exp, c.trim().getBytes(StandardCharsets.UTF_8)));
    }
}
