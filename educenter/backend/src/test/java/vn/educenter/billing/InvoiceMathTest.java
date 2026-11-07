package vn.educenter.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceMathTest {
    private static BigDecimal v(long n) { return BigDecimal.valueOf(n); }

    @Test
    void totalAppliesDiscountButNeverNegative() {
        assertThat(InvoiceMath.total(v(7_200_000), v(500_000))).isEqualByComparingTo(v(6_700_000));
        assertThat(InvoiceMath.total(v(100_000), v(200_000))).isEqualByComparingTo(v(0));
        assertThatThrownBy(() -> InvoiceMath.total(v(1), v(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusFollowsPayments() {
        assertThat(InvoiceMath.statusAfterPayment(v(1_000_000), v(0))).isEqualTo(Invoice.Status.UNPAID);
        assertThat(InvoiceMath.statusAfterPayment(v(1_000_000), v(400_000))).isEqualTo(Invoice.Status.PARTIAL);
        assertThat(InvoiceMath.statusAfterPayment(v(1_000_000), v(1_000_000))).isEqualTo(Invoice.Status.PAID);
        assertThat(InvoiceMath.remaining(v(1_000_000), v(400_000))).isEqualByComparingTo(v(600_000));
    }

    @Test
    void overdueOnlyForOpenInvoicesPastDue() {
        LocalDate today = LocalDate.of(2026, 11, 5);
        assertThat(InvoiceMath.isOverdue(Invoice.Status.UNPAID, today.minusDays(1), today)).isTrue();
        assertThat(InvoiceMath.isOverdue(Invoice.Status.PARTIAL, today.minusDays(1), today)).isTrue();
        assertThat(InvoiceMath.isOverdue(Invoice.Status.UNPAID, today, today)).isFalse();
        assertThat(InvoiceMath.isOverdue(Invoice.Status.PAID, today.minusDays(9), today)).isFalse();
        assertThat(InvoiceMath.isOverdue(Invoice.Status.CANCELLED, today.minusDays(9), today)).isFalse();
    }
}
