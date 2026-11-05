package vn.bookeasy.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Zalo ZNS (Zalo Notification Service) – bản MVP là mock: chỉ ghi log.
 *
 * Tích hợp thật:
 * 1. Đăng ký Zalo Official Account + ZNS, tạo template (ví dụ "booking_confirm" với tham số
 *    customer_name, booking_code, service, time, business) và chờ Zalo duyệt.
 * 2. Lấy access_token OA (refresh định kỳ bằng refresh_token).
 * 3. POST https://business.openapi.zalo.me/message/template
 *    header access_token, body {"phone":"84xxxxxxxxx","template_id":"...","template_data":{...},"tracking_id":"bookingId"}.
 * 4. Thay class này bằng client thật (RestClient) và cấu hình template id theo NotificationType.
 */
@Slf4j
@Component
public class ZaloZnsChannel implements NotificationChannel {
    @Override
    public String name() {
        return "ZALO_ZNS";
    }

    @Override
    public String recipient(NotificationMessage m) {
        return m.phone();
    }

    @Override
    public void send(NotificationMessage m) {
        String phone84 = m.phone().startsWith("0") ? "84" + m.phone().substring(1) : m.phone();
        log.info("[ZNS-MOCK] template={} phone={} data={}", m.type().name().toLowerCase(), phone84, m.params());
    }
}
