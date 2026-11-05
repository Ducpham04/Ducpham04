package vn.educenter.report;

import java.math.BigDecimal;
import java.util.List;
import vn.educenter.billing.BillingDtos;
import vn.educenter.classes.ClassDtos;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record MonthValue(int month, BigDecimal value) {}

    public record MonthCount(int month, long value) {}

    public record Dashboard(BigDecimal revenueThisMonth, BigDecimal revenueLastMonth, BigDecimal outstanding,
                            BigDecimal overdue, long newStudentsThisMonth, long newStudentsLastMonth, long activeStudents,
                            double attendanceRate30d, long activeClasses, List<ClassDtos.SessionView> sessionsToday,
                            List<BillingDtos.DebtItem> topDebts, List<MonthValue> revenue12Months) {}
}
