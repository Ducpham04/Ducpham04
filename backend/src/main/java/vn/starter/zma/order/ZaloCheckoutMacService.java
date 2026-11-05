package vn.starter.zma.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.starter.zma.common.config.AppProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tính / xác minh MAC cho Zalo Checkout SDK.
 * <p>Tạo đơn: data = các cặp key=value của {amount, desc, extradata, item, method} sắp xếp theo key, nối bằng '&';
 * giá trị là object/array thì JSON.stringify. mac = HMAC-SHA256(data, privateKey) dạng hex.</p>
 * <p>Callback: data = "appId=..&amount=..&description=..&orderId=..&message=..&resultCode=..&transId=.." (đúng thứ tự này).</p>
 * Nếu Zalo thay đổi công thức, chỉ cần sửa lớp này (xem docs/PITFALLS.md).
 */
@Component
public class ZaloCheckoutMacService {
    private final String privateKey;
    private final ObjectMapper objectMapper;

    public ZaloCheckoutMacService(AppProperties props, ObjectMapper objectMapper) {
        this.privateKey = props.payment().checkoutPrivateKey() == null ? "" : props.payment().checkoutPrivateKey();
        this.objectMapper = objectMapper;
    }

    public String createOrderMac(Map<String, Object> params) {
        String data = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + stringify(e.getValue()))
                .collect(Collectors.joining("&"));
        return hmac(data);
    }

    public String callbackMac(Map<String, Object> d) {
        String data = "appId=" + d.get("appId") + "&amount=" + d.get("amount") + "&description=" + d.get("description")
                + "&orderId=" + d.get("orderId") + "&message=" + d.get("message")
                + "&resultCode=" + d.get("resultCode") + "&transId=" + d.get("transId");
        return hmac(data);
    }

    public boolean verifyCallback(Map<String, Object> data, String mac) {
        if (mac == null) return false;
        return MessageDigest.isEqual(callbackMac(data).getBytes(StandardCharsets.UTF_8),
                mac.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    String hmac(String data) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }

    private String stringify(Object v) {
        if (v == null) return "";
        if (v instanceof String || v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        try {
            return objectMapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
