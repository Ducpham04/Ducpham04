package vn.starter.zma.notification;

/**
 * Cổng gửi thông báo Zalo. Chọn bằng {@code app.notification.provider}:
 * <ul>
 *   <li>{@code mock}: {@link MockNotificationGateway} – chỉ ghi log</li>
 *   <li>{@code zns}: {@link ZnsNotificationGateway} – gọi ZNS API thật</li>
 * </ul>
 */
public interface NotificationGateway {
    NotificationResult send(NotificationMessage message);
}
