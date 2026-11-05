package vn.starter.zma.feedback;

public enum FeedbackStatus {
    NEW("Mới tiếp nhận"), IN_PROGRESS("Đang xử lý"), RESOLVED("Đã xử lý"), REJECTED("Từ chối");

    private final String label;

    FeedbackStatus(String label) { this.label = label; }

    public String label() { return label; }
}
