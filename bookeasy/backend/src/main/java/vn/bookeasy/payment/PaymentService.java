package vn.bookeasy.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.booking.BookingService;
import vn.bookeasy.booking.PaymentStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    static final Pattern CODE = Pattern.compile("BE[A-Z0-9]{6}");

    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final Clock clock;

    public enum Result { APPLIED, DUPLICATE, UNMATCHED, IGNORED }

    static Optional<String> extractCode(String content) {
        if (content == null) return Optional.empty();
        // Ngân hàng thường bỏ dấu cách/ký tự đặc biệt và có thể viết thường.
        Matcher m = CODE.matcher(content.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " "));
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    @Transactional
    public Result apply(String provider, IncomingTransfer t) {
        if (t.txnId() == null || t.txnId().isBlank() || t.amount() <= 0) return Result.IGNORED;
        if (payments.existsByProviderAndProviderTxnId(provider, t.txnId())) return Result.DUPLICATE;
        Optional<Booking> booking = extractCode(t.content()).flatMap(bookings::findByCodeDetailed);
        Payment p = new Payment();
        p.setProvider(provider);
        p.setProviderTxnId(t.txnId());
        p.setAmount(t.amount());
        p.setContent(t.content() == null ? null : t.content().substring(0, Math.min(500, t.content().length())));
        p.setCreatedAt(LocalDateTime.now(clock));
        if (booking.isEmpty()) {
            p.setBusinessId(0L);
            payments.saveAndFlush(p);
            log.warn("Unmatched transfer {} from {}: {}", t.txnId(), provider, t.content());
            return Result.UNMATCHED;
        }
        Booking bk = booking.get();
        p.setBusinessId(bk.getBusinessId());
        p.setBookingId(bk.getId());
        payments.saveAndFlush(p);
        if (bk.getPaymentStatus() == PaymentStatus.PENDING) {
            bookingService.applyPayment(bk, t.amount());
            p.setMatched(true);
            return Result.APPLIED;
        }
        log.info("Transfer {} for booking {} without pending payment (status {})", t.txnId(), bk.getCode(), bk.getStatus());
        return Result.UNMATCHED;
    }
}
