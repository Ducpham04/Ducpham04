package vn.bookeasy.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingEvent;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.business.Business;
import vn.bookeasy.business.BusinessRepository;
import vn.bookeasy.config.AppProperties;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final List<NotificationChannel> channels;
    private final BookingRepository bookings;
    private final BusinessRepository businesses;
    private final NotificationLogRepository logs;
    private final AppProperties props;
    private final Clock clock;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingEvent(BookingEvent event) {
        NotificationType type = switch (event.type()) {
            case CONFIRMED -> NotificationType.CONFIRMATION;
            case CANCELLED -> NotificationType.CANCELLED;
            case RESCHEDULED -> NotificationType.RESCHEDULED;
        };
        bookings.findByIdDetailed(event.bookingId()).ifPresent(b -> notify(b, type));
    }

    public void notify(Booking b, NotificationType type) {
        Business biz = businesses.findById(b.getBusinessId()).orElseThrow();
        NotificationMessage msg = build(biz, b, type);
        for (NotificationChannel ch : channels) {
            String to = ch.recipient(msg);
            if (to == null) continue;
            String status = "SENT";
            String error = null;
            try {
                ch.send(msg);
            } catch (Exception e) {
                status = "FAILED";
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                log.warn("Send {} via {} failed for booking {}: {}", type, ch.name(), b.getCode(), error);
            }
            NotificationLog l = new NotificationLog();
            l.setBusinessId(b.getBusinessId());
            l.setBookingId(b.getId());
            l.setChannel(ch.name());
            l.setType(type.name());
            l.setRecipient(to);
            l.setStatus(status);
            l.setError(error == null ? null : error.substring(0, Math.min(error.length(), 500)));
            l.setCreatedAt(LocalDateTime.now(clock));
            logs.save(l);
        }
    }

    NotificationMessage build(Business biz, Booking b, NotificationType type) {
        String when = b.getStartAt().format(TIME);
        String headline = switch (type) {
            case CONFIRMATION -> "Lịch hẹn của bạn đã được xác nhận";
            case REMINDER_24H -> "Nhắc lịch: bạn có hẹn vào ngày mai";
            case REMINDER_2H -> "Nhắc lịch: còn 2 giờ nữa đến giờ hẹn";
            case CANCELLED -> "Lịch hẹn của bạn đã bị hủy";
            case RESCHEDULED -> "Lịch hẹn của bạn đã được đổi giờ";
        };
        String subject = "[" + biz.getName() + "] " + headline + " – " + b.getCode();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("customer_name", b.getCustomer().getFullName());
        params.put("booking_code", b.getCode());
        params.put("service", b.getService().getName());
        params.put("staff", b.getStaff().getFullName());
        params.put("time", when);
        params.put("business", biz.getName());
        params.put("address", b.getBranch().getAddress() == null ? "" : b.getBranch().getAddress());
        String manageUrl = props.publicUrl() + "/b/" + biz.getSlug() + "/tra-cuu?code=" + b.getCode();
        String color = biz.getBrandColor();
        StringBuilder rows = new StringBuilder();
        row(rows, "Mã lịch hẹn", "<span style=\"font-family:monospace;font-size:16px\">" + b.getCode() + "</span>");
        row(rows, "Dịch vụ", esc(b.getService().getName()));
        row(rows, "Nhân viên", esc(b.getStaff().getFullName()));
        row(rows, "Thời gian", when);
        row(rows, "Địa điểm", esc(b.getBranch().getName() + (b.getBranch().getAddress() == null ? "" : " – " + b.getBranch().getAddress())));
        row(rows, "Giá", String.format(Locale.forLanguageTag("vi-VN"), "%,d ₫", b.getPrice()).replace(',', '.'));
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;color:#1f2a27">
                  <p style="font-size:14px;color:#5b6b66;margin:0 0 4px">%s</p>
                  <h1 style="font-size:20px;margin:0 0 16px;color:%s">%s</h1>
                  <p style="margin:0 0 16px">Xin chào %s,</p>
                  <table style="width:100%%;border-collapse:collapse;font-size:14px">%s</table>
                  <p style="margin:20px 0"><a href="%s" style="background:%s;color:#fff;padding:10px 16px;border-radius:8px;text-decoration:none">Xem hoặc hủy lịch hẹn</a></p>
                  <p style="font-size:12px;color:#5b6b66">Email tự động từ BookEasy. Vui lòng không trả lời email này.</p>
                </div>
                """.formatted(esc(biz.getName()), color, headline, esc(b.getCustomer().getFullName()), rows, manageUrl, color);
        return new NotificationMessage(b.getBusinessId(), b.getId(), type, b.getCustomer().getEmail(),
                b.getCustomer().getPhone(), subject, html, params);
    }

    private static void row(StringBuilder sb, String k, String v) {
        sb.append("<tr><td style=\"padding:6px 0;color:#5b6b66;width:120px\">").append(k)
                .append("</td><td style=\"padding:6px 0\">").append(v).append("</td></tr>");
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
