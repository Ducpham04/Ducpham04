package vn.bookeasy.support;

import java.time.*;

/** Clock điều khiển được trong test (tua thời gian để kiểm tra hết hạn, nhắc lịch). */
public class MutableClock extends Clock {
    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start, ZoneId zone) {
        this.zone = zone;
        this.instant = start.atZone(zone).toInstant();
    }

    public void set(LocalDateTime t) {
        instant = t.atZone(zone).toInstant();
    }

    public void advance(Duration d) {
        instant = instant.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
