package vn.starter.zma.notification;

public record NotificationResult(Status status, String providerMessageId, String error) {
    public enum Status { SENT, FAILED, MOCKED, SKIPPED }

    public static NotificationResult mocked() { return new NotificationResult(Status.MOCKED, "mock", null); }
    public static NotificationResult sent(String id) { return new NotificationResult(Status.SENT, id, null); }
    public static NotificationResult failed(String err) { return new NotificationResult(Status.FAILED, null, err); }
    public static NotificationResult skipped(String why) { return new NotificationResult(Status.SKIPPED, null, why); }
}
