package vn.bookeasy.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.bookeasy.booking.BookingStatus.*;

class BookingStatusTest {
    @Test
    void allowsHappyPath() {
        assertThat(PENDING_PAYMENT.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(CHECKED_IN)).isTrue();
        assertThat(CHECKED_IN.canTransitionTo(COMPLETED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(NO_SHOW)).isTrue();
    }

    @Test
    void rejectsInvalidTransitions() {
        assertThat(PENDING_PAYMENT.canTransitionTo(CHECKED_IN)).isFalse();
        assertThat(CONFIRMED.canTransitionTo(COMPLETED)).isFalse();
        assertThat(COMPLETED.canTransitionTo(CANCELLED)).isFalse();
        assertThat(CANCELLED.canTransitionTo(CONFIRMED)).isFalse();
        assertThat(NO_SHOW.canTransitionTo(CHECKED_IN)).isFalse();
    }

    @Test
    void activeStatesHoldSlots() {
        assertThat(CONFIRMED.isActive()).isTrue();
        assertThat(CANCELLED.isActive()).isFalse();
        assertThat(COMPLETED.isActive()).isFalse();
    }
}
