package vn.starter.zma.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;
import vn.starter.zma.common.util.PageRequests;
import vn.starter.zma.content.Product;
import vn.starter.zma.content.ProductRepository;
import vn.starter.zma.order.OrderDtos.*;
import vn.starter.zma.security.AuthUser;
import vn.starter.zma.user.UserRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    static final Duration PENDING_TIMEOUT = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentProvider paymentProvider;
    private final ZaloCheckoutMacService macService;
    private final OrderMapper mapper;
    private final ObjectMapper objectMapper;
    private final AppProperties props;
    private final Clock clock;

    /** Tạo đơn: giá lấy từ DB (không tin client), khoá dòng sản phẩm để trừ tồn kho an toàn. */
    @Transactional
    public CreateOrderResponse create(Long userId, CreateOrderRequest req) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        req.items().forEach(i -> qty.merge(i.productId(), i.quantity(), Integer::sum));
        Map<Long, Product> products = productRepository.findAllForUpdate(qty.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setCode(newCode());
        long amount = 0;
        for (var e : qty.entrySet()) {
            Product p = products.get(e.getKey());
            if (p == null || !p.isActive()) throw AppException.notFound("sản phẩm #" + e.getKey());
            if (p.getStock() < e.getValue()) throw new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm \"" + p.getName() + "\" không đủ hàng");
            p.setStock(p.getStock() - e.getValue());
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setProductName(p.getName());
            item.setUnitPrice(p.getPrice());
            item.setQuantity(e.getValue());
            order.getItems().add(item);
            amount = Math.addExact(amount, Math.multiplyExact(p.getPrice(), e.getValue()));
        }
        order.setAmount(amount);
        String firstName = order.getItems().get(0).getProductName();
        String desc = order.getItems().size() == 1 ? "Thanh toán " + firstName
                : "Thanh toán " + firstName + " và " + (order.getItems().size() - 1) + " sản phẩm khác";
        order.setDescription(desc.length() > 250 ? desc.substring(0, 250) : desc);
        orderRepository.save(order);
        CheckoutParams checkout = paymentProvider.createCheckout(order);
        return new CreateOrderResponse(mapper.toDto(order), checkout, paymentProvider.name());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> mine(Long userId, int page, int size) {
        return PageResponse.of(orderRepository.findByUserId(userId, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
                mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> search(OrderStatus status, int page, int size) {
        return PageResponse.of(orderRepository.search(status, PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
                mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long id, AuthUser viewer) {
        Order o = orderRepository.findDetailById(id).orElseThrow(() -> AppException.notFound("đơn hàng"));
        if (!viewer.isAdmin() && !o.getUser().getId().equals(viewer.id())) throw AppException.notFound("đơn hàng");
        return mapper.toDto(o);
    }

    /** Chỉ dùng ở chế độ mock: giả lập Zalo báo thanh toán thành công. */
    @Transactional
    public OrderDto mockPay(Long id, AuthUser viewer, boolean success) {
        if (!props.payment().isMock()) throw new AppException(ErrorCode.FEATURE_DISABLED);
        Order o = orderRepository.findByIdForUpdate(id).orElseThrow(() -> AppException.notFound("đơn hàng"));
        if (!o.getUser().getId().equals(viewer.id())) throw AppException.notFound("đơn hàng");
        if (success) markPaid(o, "MOCK", "MOCK-" + o.getCode(), "MOCK-" + System.currentTimeMillis());
        else markFailed(o);
        return mapper.toDto(orderRepository.findDetailById(id).orElseThrow());
    }

    /**
     * Xử lý callback Checkout SDK. Idempotent: đơn đã PAID thì bỏ qua.
     * @return true nếu hợp lệ (đã ghi nhận), false nếu sai mac / không tìm thấy đơn.
     */
    @Transactional
    public boolean handleZaloNotify(Map<String, Object> data, String mac) {
        if (data == null || !macService.verifyCallback(data, mac)) {
            log.warn("Zalo payment notify: invalid mac");
            return false;
        }
        String orderCode = extractOrderCode(data.get("extradata"));
        if (orderCode == null) {
            log.warn("Zalo payment notify: missing orderCode in extradata");
            return false;
        }
        Optional<Order> found = orderRepository.findByCodeForUpdate(orderCode);
        if (found.isEmpty()) return false;
        Order o = found.get();
        long amount = Long.parseLong(String.valueOf(data.get("amount")));
        if (amount != o.getAmount()) {
            log.error("Zalo payment notify: amount mismatch for {} ({} != {})", orderCode, amount, o.getAmount());
            return false;
        }
        int resultCode = Integer.parseInt(String.valueOf(data.getOrDefault("resultCode", "0")));
        String method = String.valueOf(data.getOrDefault("method", "ZALOPAY"));
        if (resultCode == 1) markPaid(o, method, String.valueOf(data.get("orderId")), String.valueOf(data.get("transId")));
        else markFailed(o);
        return true;
    }

    /** Huỷ đơn PENDING quá hạn và hoàn tồn kho. */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    @Transactional
    public void cancelStalePending() {
        List<Long> ids = orderRepository.findStalePendingIds(clock.instant().minus(PENDING_TIMEOUT));
        for (Long id : ids) {
            orderRepository.findByIdForUpdate(id).filter(o -> o.getStatus() == OrderStatus.PENDING).ifPresent(o -> {
                o.setStatus(OrderStatus.CANCELLED);
                restoreStock(o);
            });
        }
        if (!ids.isEmpty()) log.info("Cancelled {} stale pending orders", ids.size());
    }

    private void markPaid(Order o, String method, String providerOrderId, String transId) {
        if (o.getStatus() == OrderStatus.PAID) return;
        if (o.getStatus() != OrderStatus.PENDING) {
            log.warn("Payment received for non-pending order {} (status {})", o.getCode(), o.getStatus());
        }
        o.setStatus(OrderStatus.PAID);
        o.setPaymentMethod(method);
        o.setProviderOrderId(providerOrderId);
        o.setProviderTransId(transId);
        o.setPaidAt(clock.instant());
    }

    private void markFailed(Order o) {
        if (o.getStatus() != OrderStatus.PENDING) return;
        o.setStatus(OrderStatus.FAILED);
        restoreStock(o);
    }

    private void restoreStock(Order o) {
        List<Long> ids = o.getItems().stream().map(i -> i.getProduct().getId()).toList();
        Map<Long, Product> products = productRepository.findAllForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        o.getItems().forEach(i -> {
            Product p = products.get(i.getProduct().getId());
            if (p != null) p.setStock(p.getStock() + i.getQuantity());
        });
    }

    private String extractOrderCode(Object extradata) {
        if (extradata == null) return null;
        String s = String.valueOf(extradata);
        if (s.contains("%7B") || s.contains("%22")) s = URLDecoder.decode(s, StandardCharsets.UTF_8);
        try {
            JsonNode n = objectMapper.readTree(s);
            String code = n.path("orderCode").asText(null);
            return code != null && code.matches("[A-Z0-9-]{1,32}") ? code : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String newCode() {
        return "DH" + CODE_DATE.format(Instant.now(clock)) + String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
