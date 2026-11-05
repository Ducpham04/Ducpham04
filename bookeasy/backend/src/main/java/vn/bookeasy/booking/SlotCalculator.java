package vn.bookeasy.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Hàm thuần tính các giờ bắt đầu khả dụng – tách riêng để unit test dễ dàng. */
public final class SlotCalculator {
    public static final int UNIT_MINUTES = 15;

    private SlotCalculator() {
    }

    public record Window(LocalTime start, LocalTime end) {
        public Window intersect(Window other) {
            LocalTime s = start.isAfter(other.start) ? start : other.start;
            LocalTime e = end.isBefore(other.end) ? end : other.end;
            return s.isBefore(e) ? new Window(s, e) : null;
        }
    }

    /**
     * @param date      ngày tính
     * @param windows   các khoảng làm việc (đã giao với giờ mở cửa)
     * @param busy      các mốc slot_start đã bị khóa của nhân viên
     * @param duration  thời lượng dịch vụ (bội số 15)
     * @param earliest  thời điểm sớm nhất được phép đặt (now + min lead)
     */
    public static List<LocalTime> availableStarts(LocalDate date, List<Window> windows, Set<LocalDateTime> busy,
                                                  int duration, LocalDateTime earliest) {
        List<LocalTime> result = new ArrayList<>();
        for (Window w : windows) {
            LocalDateTime t = alignUp(date.atTime(w.start()));
            LocalDateTime windowEnd = date.atTime(w.end());
            while (!t.plusMinutes(duration).isAfter(windowEnd)) {
                if (!t.isBefore(earliest) && isFree(t, duration, busy)) {
                    result.add(t.toLocalTime());
                }
                t = t.plusMinutes(UNIT_MINUTES);
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    public static boolean isFree(LocalDateTime start, int duration, Set<LocalDateTime> busy) {
        for (LocalDateTime u : units(start, duration)) {
            if (busy.contains(u)) return false;
        }
        return true;
    }

    /** Các đơn vị 15 phút mà một booking chiếm giữ. */
    public static List<LocalDateTime> units(LocalDateTime start, int duration) {
        List<LocalDateTime> list = new ArrayList<>(duration / UNIT_MINUTES);
        for (int m = 0; m < duration; m += UNIT_MINUTES) list.add(start.plusMinutes(m));
        return list;
    }

    static LocalDateTime alignUp(LocalDateTime t) {
        int rem = t.getMinute() % UNIT_MINUTES;
        LocalDateTime base = t.withSecond(0).withNano(0);
        return rem == 0 && t.getSecond() == 0 && t.getNano() == 0 ? base : base.plusMinutes(UNIT_MINUTES - rem);
    }

    public static boolean isAligned(LocalDateTime t) {
        return t.getMinute() % UNIT_MINUTES == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }
}
