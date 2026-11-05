package vn.bookeasy.notification;

public interface NotificationChannel {
    String name();

    /** @return người nhận nếu kênh áp dụng được cho tin này, null nếu bỏ qua. */
    String recipient(NotificationMessage message);

    void send(NotificationMessage message) throws Exception;
}
