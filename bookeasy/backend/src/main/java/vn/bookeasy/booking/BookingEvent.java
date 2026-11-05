package vn.bookeasy.booking;

public record BookingEvent(Long bookingId, Type type) {
    public enum Type { CONFIRMED, CANCELLED, RESCHEDULED }
}
