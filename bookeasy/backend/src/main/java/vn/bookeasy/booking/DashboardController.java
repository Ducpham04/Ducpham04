package vn.bookeasy.booking;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.bookeasy.auth.AuthPrincipal;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.booking.BookingDtos.BookingResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Tổng quan")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
public class DashboardController {
    private final BookingService bookingService;
    private final Clock clock;

    public record TodayResponse(LocalDate date, long total, long pendingPayment, long checkedIn, long completed,
                                long cancelled, long noShow, long expectedRevenue, List<BookingResponse> upcoming,
                                List<BookingResponse> needsAttention) {
    }

    @GetMapping("/today")
    public TodayResponse today() {
        AuthPrincipal user = CurrentUser.get();
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<BookingResponse> list = bookingService.search(user, today.atStartOfDay(), today.plusDays(1).atStartOfDay(),
                null, null, null, null);
        long count = list.stream().filter(b -> b.status() != BookingStatus.CANCELLED).count();
        List<BookingResponse> upcoming = list.stream()
                .filter(b -> (b.status() == BookingStatus.CONFIRMED || b.status() == BookingStatus.CHECKED_IN
                        || b.status() == BookingStatus.PENDING_PAYMENT) && b.endAt().isAfter(now))
                .toList();
        List<BookingResponse> attention = list.stream()
                .filter(b -> (b.status() == BookingStatus.CONFIRMED && b.startAt().isBefore(now.minusMinutes(15)))
                        || (b.status() == BookingStatus.CHECKED_IN && b.endAt().isBefore(now)))
                .toList();
        long revenue = list.stream().filter(b -> b.status() != BookingStatus.CANCELLED && b.status() != BookingStatus.NO_SHOW)
                .mapToLong(BookingResponse::price).sum();
        return new TodayResponse(today, count,
                countOf(list, BookingStatus.PENDING_PAYMENT), countOf(list, BookingStatus.CHECKED_IN),
                countOf(list, BookingStatus.COMPLETED), countOf(list, BookingStatus.CANCELLED),
                countOf(list, BookingStatus.NO_SHOW), revenue, upcoming, attention);
    }

    private static long countOf(List<BookingResponse> list, BookingStatus s) {
        return list.stream().filter(b -> b.status() == s).count();
    }
}
