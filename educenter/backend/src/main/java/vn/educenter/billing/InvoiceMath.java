package vn.educenter.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Quy tắc tiền & trạng thái hóa đơn (port từ billing-utils.ts của bản cũ). Thuần, dễ unit test. */
public final class InvoiceMath {
    private InvoiceMath() {
    }

    public static BigDecimal total(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal d = discount == null ? BigDecimal.ZERO : discount;
        if (d.signum() < 0) throw new IllegalArgumentException("discount < 0");
        return subtotal.subtract(d).max(BigDecimal.ZERO);
    }

    public static BigDecimal remaining(BigDecimal total, BigDecimal paid) {
        return total.subtract(paid == null ? BigDecimal.ZERO : paid).max(BigDecimal.ZERO);
    }

    public static Invoice.Status statusAfterPayment(BigDecimal total, BigDecimal paid) {
        if (remaining(total, paid).signum() == 0) return Invoice.Status.PAID;
        if (paid != null && paid.signum() > 0) return Invoice.Status.PARTIAL;
        return Invoice.Status.UNPAID;
    }

    /** Quá hạn = chưa thu đủ, chưa hủy và hạn thanh toán đã qua. */
    public static boolean isOverdue(Invoice.Status status, LocalDate dueDate, LocalDate today) {
        return (status == Invoice.Status.UNPAID || status == Invoice.Status.PARTIAL) && dueDate != null && dueDate.isBefore(today);
    }
}
