package vn.starter.zma.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.starter.zma.order.OrderDtos.CheckoutParams;

import java.util.*;

public class ZaloCheckoutPaymentProvider implements PaymentProvider {
    private final ZaloCheckoutMacService macService;
    private final ObjectMapper objectMapper;
    private final String method;

    public ZaloCheckoutPaymentProvider(ZaloCheckoutMacService macService, ObjectMapper objectMapper, String method) {
        this.macService = macService;
        this.objectMapper = objectMapper;
        this.method = method == null ? "" : method;
    }

    @Override
    public String name() {
        return "zalo";
    }

    @Override
    public CheckoutParams createCheckout(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem i : order.getItems()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(i.getProduct().getId()));
            m.put("amount", i.getUnitPrice() * i.getQuantity());
            items.add(m);
        }
        String extradata;
        try {
            extradata = objectMapper.writeValueAsString(Map.of("orderCode", order.getCode()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("amount", order.getAmount());
        params.put("desc", order.getDescription());
        params.put("item", items);
        params.put("extradata", extradata);
        params.put("method", method);
        return new CheckoutParams(order.getAmount(), order.getDescription(), items, extradata, method,
                macService.createOrderMac(params));
    }
}
