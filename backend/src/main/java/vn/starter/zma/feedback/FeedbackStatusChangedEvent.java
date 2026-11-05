package vn.starter.zma.feedback;

public record FeedbackStatusChangedEvent(Long feedbackId, Long userId, String phone, String code, String title,
                                         FeedbackStatus status, String adminReply) {}
