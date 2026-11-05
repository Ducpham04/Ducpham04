package vn.starter.zma.notification;

import java.util.Map;

/**
 * Thông điệp gửi qua Zalo.
 * @param channel   ZNS (gửi theo SĐT, cần template được duyệt) hoặc OA (gửi theo user_id OA, user phải quan tâm OA)
 * @param recipient SĐT dạng 84xxxxxxxxx (ZNS) hoặc user_id theo OA (OA)
 */
public record NotificationMessage(Channel channel, Long userId, String recipient, String templateId,
                                  Map<String, String> data, String fallbackText) {
    public enum Channel { ZNS, OA }
}
