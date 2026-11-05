package vn.educenter.notification;

import java.util.Map;

/**
 * Gửi tin Zalo ZNS (Zalo Notification Service) theo template đã được duyệt.
 *
 * <p>Tích hợp thật (xem README): POST https://business.openapi.zalo.me/message/template với header
 * {@code access_token: <OA access token>}, body {@code {"phone":"84xxxxxxxxx","template_id":"...","template_data":{...},
 * "tracking_id":"..."}}. Cần: Zalo OA đã xác thực, ứng dụng Zalo for Developers, template được duyệt, cơ chế làm mới
 * access token (refresh token hết hạn sau 3 tháng).</p>
 */
public interface ZaloZnsClient {

    record Result(boolean success, boolean mock, String messageId, String error) {}

    Result sendTemplate(String phone, String templateId, Map<String, String> data, String trackingId);
}
