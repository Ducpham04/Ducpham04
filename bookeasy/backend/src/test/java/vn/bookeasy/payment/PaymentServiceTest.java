package vn.bookeasy.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.booking.BookingService;
import vn.bookeasy.booking.PaymentStatus;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    PaymentRepository payments;
    @Mock
    BookingRepository bookings;
    @Mock
    BookingService bookingService;
    @Spy
    Clock clock = Clock.systemDefaultZone();
    @InjectMocks
    PaymentService service;

    @Test
    void duplicateTransactionIsIgnored() {
        when(payments.existsByProviderAndProviderTxnId("sepay", "T1")).thenReturn(true);
        assertThat(service.apply("sepay", new IncomingTransfer("T1", 1000, "BEAB12CD"))).isEqualTo(PaymentService.Result.DUPLICATE);
        verifyNoInteractions(bookingService);
    }

    @Test
    void unmatchedTransferIsRecordedWithoutBooking() {
        when(bookings.findByCodeDetailed(any())).thenReturn(Optional.empty());
        assertThat(service.apply("sepay", new IncomingTransfer("T2", 1000, "BEZZZZZZ"))).isEqualTo(PaymentService.Result.UNMATCHED);
        verify(payments).saveAndFlush(argThat(p -> p.getBookingId() == null && p.getBusinessId() == 0L));
        verify(bookingService, never()).applyPayment(any(), anyLong());
    }

    @Test
    void pendingBookingReceivesPayment() {
        Booking bk = new Booking();
        bk.setId(9L);
        bk.setBusinessId(3L);
        bk.setCode("BEAB12CD");
        bk.setPaymentStatus(PaymentStatus.PENDING);
        when(bookings.findByCodeDetailed("BEAB12CD")).thenReturn(Optional.of(bk));
        assertThat(service.apply("casso", new IncomingTransfer("T3", 84000, "ck beab12cd"))).isEqualTo(PaymentService.Result.APPLIED);
        verify(bookingService).applyPayment(bk, 84000);
    }

    @Test
    void invalidTransferIgnored() {
        assertThat(service.apply("mock", new IncomingTransfer("", 1000, "x"))).isEqualTo(PaymentService.Result.IGNORED);
        assertThat(service.apply("mock", new IncomingTransfer("T", 0, "x"))).isEqualTo(PaymentService.Result.IGNORED);
    }
}
