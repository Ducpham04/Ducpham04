package vn.starter.zma.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.util.PhoneUtils;
import vn.starter.zma.notification.NotificationMessage;
import vn.starter.zma.notification.NotificationService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Gửi ZNS cho người dân khi phản ánh được cập nhật – chỉ sau khi transaction commit thành công. */
@Component
@RequiredArgsConstructor
public class FeedbackNotificationListener {
    private final NotificationService notificationService;
    private final AppProperties props;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(FeedbackStatusChangedEvent e) {
        // Tên tham số template phải KHỚP với template ZNS đã đăng ký trên Zalo Cloud Account
        Map<String, String> data = new LinkedHashMap<>();
        data.put("feedback_code", e.code());
        data.put("title", e.title());
        data.put("status", e.status().label());
        data.put("reply", e.adminReply() == null ? "" : e.adminReply());
        String text = "Phản ánh %s của bạn đã chuyển sang trạng thái: %s".formatted(e.code(), e.status().label());
        notificationService.send(new NotificationMessage(NotificationMessage.Channel.ZNS, e.userId(),
                PhoneUtils.toInternational(e.phone()), props.notification().feedbackTemplateId(), data, text));
    }
}
