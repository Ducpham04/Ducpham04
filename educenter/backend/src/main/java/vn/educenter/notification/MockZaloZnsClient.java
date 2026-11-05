package vn.educenter.notification;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bản mock: không gọi Zalo, chỉ ghi log. Bật khi app.zalo.mode=mock (mặc định). */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.zalo", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockZaloZnsClient implements ZaloZnsClient {

    @Override
    public Result sendTemplate(String phone, String templateId, Map<String, String> data, String trackingId) {
        log.info("[ZNS MOCK] phone={} template={} data={} tracking={}", maskPhone(phone), templateId, data, trackingId);
        return new Result(true, true, "mock-" + UUID.randomUUID(), null);
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "*".repeat(phone.length() - 3) + phone.substring(phone.length() - 3);
    }
}
