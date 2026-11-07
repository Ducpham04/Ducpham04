package vn.bookeasy.notification;

import java.util.Map;

/**
 * Nội dung thông báo độc lập kênh. {@code params} dùng cho kênh template (Zalo ZNS),
 * {@code subject/html} dùng cho email.
 */
public record NotificationMessage(Long businessId, Long bookingId, NotificationType type, String email, String phone,
                                  String subject, String html, Map<String, String> params) {
}
