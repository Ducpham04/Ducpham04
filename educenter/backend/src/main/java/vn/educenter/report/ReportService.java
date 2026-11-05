package vn.educenter.report;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.attendance.Attendance;
import vn.educenter.attendance.AttendanceRepository;
import vn.educenter.attendance.AttendanceService;
import vn.educenter.billing.BillingService;
import vn.educenter.billing.InvoiceRepository;
import vn.educenter.billing.PaymentRepository;
import vn.educenter.classes.ClassGroup;
import vn.educenter.classes.ClassGroupRepository;
import vn.educenter.classes.SessionService;
import vn.educenter.config.TimeConfig;
import vn.educenter.security.CurrentUser;
import vn.educenter.student.Student;
import vn.educenter.student.StudentRepository;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassGroupRepository classRepository;
    private final SessionService sessionService;
    private final BillingService billingService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReportDtos.Dashboard dashboard() {
        Long tenantId = CurrentUser.tenantId();
        LocalDate today = LocalDate.now(clock);
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        var debts = billingService.debts();
        List<ReportDtos.MonthValue> last12 = new ArrayList<>();
        YearMonth start = thisMonth.minusMonths(11);
        Map<YearMonth, BigDecimal> revenueByMonth = revenueByMonth(tenantId, start.atDay(1), thisMonth.atEndOfMonth());
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            last12.add(new ReportDtos.MonthValue(ym.getYear() * 100 + ym.getMonthValue(), revenueByMonth.getOrDefault(ym, BigDecimal.ZERO)));
        }
        return new ReportDtos.Dashboard(
                revenueByMonth.getOrDefault(thisMonth, BigDecimal.ZERO),
                revenueByMonth.getOrDefault(lastMonth, BigDecimal.ZERO),
                debts.totalOutstanding(), debts.totalOverdue(),
                studentRepository.countByTenantIdAndJoinedAtBetween(tenantId, thisMonth.atDay(1), thisMonth.atEndOfMonth()),
                studentRepository.countByTenantIdAndJoinedAtBetween(tenantId, lastMonth.atDay(1), lastMonth.atEndOfMonth()),
                studentRepository.countByTenantIdAndStatus(tenantId, Student.Status.ACTIVE),
                attendanceRate(tenantId, today.minusDays(29), today),
                classRepository.findByTenantIdAndStatusIn(tenantId, EnumSet.of(ClassGroup.Status.ONGOING, ClassGroup.Status.PLANNED)).size(),
                sessionService.today(),
                debts.items().stream().limit(5).toList(),
                last12);
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.MonthValue> revenue(int year) {
        Long tenantId = CurrentUser.tenantId();
        Map<YearMonth, BigDecimal> map = revenueByMonth(tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        List<ReportDtos.MonthValue> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) result.add(new ReportDtos.MonthValue(m, map.getOrDefault(YearMonth.of(year, m), BigDecimal.ZERO)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.MonthCount> newStudents(int year) {
        Long tenantId = CurrentUser.tenantId();
        long[] counts = new long[13];
        studentRepository.joinedDates(tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .forEach(d -> counts[d.getMonthValue()]++);
        List<ReportDtos.MonthCount> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) result.add(new ReportDtos.MonthCount(m, counts[m]));
        return result;
    }

    double attendanceRate(Long tenantId, LocalDate from, LocalDate to) {
        long attended = 0;
        long total = 0;
        for (Object[] r : attendanceRepository.statsOverall(tenantId, from, to)) {
            long n = (Long) r[1];
            total += n;
            if (((Attendance.Status) r[0]).attended()) attended += n;
        }
        return AttendanceService.rate(attended, total);
    }

    private Map<YearMonth, BigDecimal> revenueByMonth(Long tenantId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(TimeConfig.VN).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(TimeConfig.VN).toInstant();
        Map<YearMonth, BigDecimal> map = new HashMap<>();
        for (Object[] r : paymentRepository.amountsBetween(tenantId, start, end)) {
            YearMonth ym = YearMonth.from(((Instant) r[0]).atZone(TimeConfig.VN));
            map.merge(ym, (BigDecimal) r[1], BigDecimal::add);
        }
        return map;
    }
}
