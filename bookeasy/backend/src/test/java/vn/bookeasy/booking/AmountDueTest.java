package vn.bookeasy.booking;

import org.junit.jupiter.api.Test;
import vn.bookeasy.business.Business;
import vn.bookeasy.business.PaymentMode;

import static org.assertj.core.api.Assertions.assertThat;

class AmountDueTest {
    private Business biz(PaymentMode mode, int pct) {
        Business b = new Business();
        b.setPaymentMode(mode);
        b.setDepositPercent(pct);
        b.setBankBin("970436");
        b.setBankAccountNo("123");
        return b;
    }

    @Test
    void depositRoundsUpToThousand() {
        assertThat(BookingService.amountDue(biz(PaymentMode.DEPOSIT, 30), 280_000)).isEqualTo(84_000);
        assertThat(BookingService.amountDue(biz(PaymentMode.DEPOSIT, 30), 150_500)).isEqualTo(46_000);
    }

    @Test
    void fullPaymentAndNone() {
        assertThat(BookingService.amountDue(biz(PaymentMode.FULL, 30), 450_000)).isEqualTo(450_000);
        assertThat(BookingService.amountDue(biz(PaymentMode.NONE, 30), 450_000)).isZero();
        assertThat(BookingService.amountDue(biz(PaymentMode.DEPOSIT, 30), 0)).isZero();
    }

    @Test
    void noPaymentWhenBankMissing() {
        Business b = biz(PaymentMode.FULL, 30);
        b.setBankAccountNo(null);
        assertThat(BookingService.amountDue(b, 450_000)).isZero();
    }
}
