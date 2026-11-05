package vn.bookeasy.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.booking.BookingStatus;
import vn.bookeasy.common.ApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final BookingRepository bookings;

    public record DailyPoint(LocalDate date, long revenue, long completed) {
    }

    public record RankItem(Long id, String name, long count, long revenue) {
    }

    public record Summary(LocalDate from, LocalDate to, long totalBookings, long completed, long cancelled,
                          long noShow, long upcoming, long revenue, long averageTicket, double noShowRate,
                          double cancelRate, List<DailyPoint> daily, List<RankItem> topServices,
                          List<RankItem> topStaff) {
    }

    @Transactional(readOnly = true)
    public Summary summary(Long bid, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw ApiException.badRequest("INVALID_RANGE", "Ngày kết thúc phải sau ngày bắt đầu");
        if (ChronoUnit.DAYS.between(from, to) > 366) throw ApiException.badRequest("RANGE_TOO_LARGE", "Tối đa 1 năm");
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        for (Object[] r : bookings.countByStatus(bid, start, end)) counts.put((BookingStatus) r[0], ((Number) r[1]).longValue());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long completed = counts.getOrDefault(BookingStatus.COMPLETED, 0L);
        long cancelled = counts.getOrDefault(BookingStatus.CANCELLED, 0L);
        long noShow = counts.getOrDefault(BookingStatus.NO_SHOW, 0L);
        long upcoming = counts.getOrDefault(BookingStatus.CONFIRMED, 0L) + counts.getOrDefault(BookingStatus.PENDING_PAYMENT, 0L)
                + counts.getOrDefault(BookingStatus.CHECKED_IN, 0L);

        TreeMap<LocalDate, long[]> daily = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) daily.put(d, new long[2]);
        long revenue = 0;
        for (Object[] r : bookings.revenueRows(bid, BookingStatus.COMPLETED, start, end)) {
            LocalDate d = ((LocalDateTime) r[0]).toLocalDate();
            long price = ((Number) r[1]).longValue();
            revenue += price;
            long[] acc = daily.computeIfAbsent(d, k -> new long[2]);
            acc[0] += price;
            acc[1]++;
        }
        long showed = completed + noShow;
        return new Summary(from, to, total, completed, cancelled, noShow, upcoming, revenue,
                completed == 0 ? 0 : revenue / completed,
                showed == 0 ? 0 : round((double) noShow / showed),
                total == 0 ? 0 : round((double) cancelled / total),
                daily.entrySet().stream().map(e -> new DailyPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList(),
                rank(bookings.topServices(bid, BookingStatus.COMPLETED, start, end)),
                rank(bookings.topStaff(bid, BookingStatus.COMPLETED, start, end)));
    }

    private static List<RankItem> rank(List<Object[]> rows) {
        return rows.stream().limit(10).map(r -> new RankItem((Long) r[0], (String) r[1],
                ((Number) r[2]).longValue(), r[3] == null ? 0 : ((Number) r[3]).longValue())).toList();
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
