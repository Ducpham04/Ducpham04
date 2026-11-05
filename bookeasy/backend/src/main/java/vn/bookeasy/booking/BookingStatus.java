package vn.bookeasy.booking;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW;

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(CHECKED_IN, NO_SHOW, CANCELLED),
            CHECKED_IN, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(BookingStatus.class),
            CANCELLED, EnumSet.noneOf(BookingStatus.class),
            NO_SHOW, EnumSet.noneOf(BookingStatus.class));

    /** Các trạng thái đang giữ chỗ (slot bị khóa). */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(PENDING_PAYMENT, CONFIRMED, CHECKED_IN);

    public boolean canTransitionTo(BookingStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
