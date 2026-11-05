package vn.educenter.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Sinh danh sách buổi học từ lịch tuần trong khoảng ngày (tối đa MAX_DAYS ngày). */
public final class SessionGenerator {

    public static final int MAX_DAYS = 180;

    private SessionGenerator() {
    }

    public record Occurrence(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    /**
     * @param from      ngày bắt đầu sinh (bao gồm)
     * @param classEnd  ngày kết thúc lớp, null = không giới hạn (cắt ở MAX_DAYS)
     * @param skip      các cặp (ngày, giờ bắt đầu) đã có buổi, không sinh lại
     */
    public static List<Occurrence> generate(LocalDate from, LocalDate classEnd, Collection<ConflictDetector.Slot> slots,
                                            Set<String> skip) {
        List<Occurrence> result = new ArrayList<>();
        LocalDate hardEnd = from.plusDays(MAX_DAYS - 1);
        LocalDate end = classEnd == null || classEnd.isAfter(hardEnd) ? hardEnd : classEnd;
        for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue();
            for (ConflictDetector.Slot slot : slots) {
                if (slot.dayOfWeek() == dow && !skip.contains(key(d, slot.startTime()))) {
                    result.add(new Occurrence(d, slot.startTime(), slot.endTime()));
                }
            }
        }
        return result;
    }

    public static String key(LocalDate date, LocalTime start) {
        return date + "T" + start;
    }
}
