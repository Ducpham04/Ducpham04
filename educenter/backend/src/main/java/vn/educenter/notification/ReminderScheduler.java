package vn.educenter.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "${app.reminders.session-cron}", zone = "Asia/Ho_Chi_Minh")
    public void sessionReminders() {
        var r = reminderService.runSessionReminders(null);
        log.info("Nhắc lịch học: gửi {}, lỗi {}, bỏ qua {}", r.sent(), r.failed(), r.skipped());
    }

    @Scheduled(cron = "${app.reminders.fee-cron}", zone = "Asia/Ho_Chi_Minh")
    public void feeReminders() {
        var r = reminderService.runFeeReminders(null);
        log.info("Nhắc học phí: gửi {}, lỗi {}, bỏ qua {}", r.sent(), r.failed(), r.skipped());
    }
}
