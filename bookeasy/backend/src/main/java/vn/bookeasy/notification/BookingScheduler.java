package vn.bookeasy.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.booking.BookingService;
import vn.bookeasy.booking.BookingStatus;

import java.time.Clock;
import java.time.LocalDateTime;

/** Chạy mỗi phút: nhắc lịch trước 24h / 2h và hủy booking quá hạn thanh toán. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final NotificationService notifications;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.scheduler.delay-ms:60000}", initialDelayString = "${app.scheduler.initial-delay-ms:30000}")
    public void run() {
        try {
            expirePendingPayments();
            sendReminders();
        } catch (Exception e) {
            log.error("Scheduler failed", e);
        }
    }

    public void expirePendingPayments() {
        for (Long id : bookings.findExpiredPending(BookingStatus.PENDING_PAYMENT, LocalDateTime.now(clock))) {
            bookingService.expire(id);
        }
    }

    @Transactional
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (Booking b : bookings.dueFor24hReminder(BookingStatus.CONFIRMED, now.plusHours(2), now.plusHours(24))) {
            b.setReminder24hSentAt(now);
            // Đặt trong vòng 24h trước giờ hẹn thì email xác nhận đã đủ, không nhắc 24h.
            if (b.getCreatedAt().isBefore(b.getStartAt().minusHours(24))) {
                notifications.notify(b, NotificationType.REMINDER_24H);
                sent++;
            }
        }
        for (Booking b : bookings.dueFor2hReminder(BookingStatus.CONFIRMED, now, now.plusHours(2))) {
            b.setReminder2hSentAt(now);
            if (b.getCreatedAt().isBefore(b.getStartAt().minusHours(2))) {
                notifications.notify(b, NotificationType.REMINDER_2H);
                sent++;
            }
        }
        return sent;
    }
}
