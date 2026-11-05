package vn.educenter.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Logic thuần (không phụ thuộc DB) để phát hiện trùng phòng / trùng giáo viên giữa các lịch tuần. */
public final class ConflictDetector {

    private static final String[] DAY_LABELS = {"", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

    private ConflictDetector() {
    }

    public enum ConflictType { ROOM, TEACHER }

    public record Slot(int dayOfWeek, LocalTime startTime, LocalTime endTime) {}

    public record ExistingSlot(Long classId, String classCode, String className, Long roomId, Long teacherId,
                               LocalDate startDate, LocalDate endDate, int dayOfWeek, LocalTime startTime, LocalTime endTime) {}

    public record Conflict(ConflictType type, Long classId, String classCode, String className, int dayOfWeek,
                           LocalTime startTime, LocalTime endTime, String message) {}

    public static String dayLabel(int dow) {
        return dow >= 1 && dow <= 7 ? DAY_LABELS[dow] : "?";
    }

    public static boolean timesOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /** Khoảng ngày [aStart, aEnd] và [bStart, bEnd] giao nhau; end null = không giới hạn. */
    public static boolean datesOverlap(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        boolean aBeforeB = aEnd != null && aEnd.isBefore(bStart);
        boolean bBeforeA = bEnd != null && bEnd.isBefore(aStart);
        return !aBeforeB && !bBeforeA;
    }

    /** Kiểm tra các slot của chính lớp: giờ hợp lệ và không chồng nhau. */
    public static List<String> validateOwnSlots(List<Slot> slots) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.dayOfWeek() < 1 || s.dayOfWeek() > 7) errors.add("Thứ trong tuần không hợp lệ");
            else if (!s.endTime().isAfter(s.startTime())) errors.add(dayLabel(s.dayOfWeek()) + ": giờ kết thúc phải sau giờ bắt đầu");
            for (int j = i + 1; j < slots.size(); j++) {
                Slot o = slots.get(j);
                if (s.dayOfWeek() == o.dayOfWeek() && timesOverlap(s.startTime(), s.endTime(), o.startTime(), o.endTime())) {
                    errors.add(dayLabel(s.dayOfWeek()) + ": các khung giờ của lớp đang chồng lên nhau");
                }
            }
        }
        return errors;
    }

    public static List<Conflict> detect(Long roomId, Long teacherId, LocalDate startDate, LocalDate endDate,
                                        Collection<Slot> slots, Collection<ExistingSlot> existing) {
        List<Conflict> result = new ArrayList<>();
        for (Slot slot : slots) {
            for (ExistingSlot ex : existing) {
                if (slot.dayOfWeek() != ex.dayOfWeek()) continue;
                if (!timesOverlap(slot.startTime(), slot.endTime(), ex.startTime(), ex.endTime())) continue;
                if (!datesOverlap(startDate, endDate, ex.startDate(), ex.endDate())) continue;
                String when = dayLabel(ex.dayOfWeek()) + ", " + ex.startTime() + "–" + ex.endTime();
                if (roomId != null && roomId.equals(ex.roomId())) {
                    result.add(new Conflict(ConflictType.ROOM, ex.classId(), ex.classCode(), ex.className(), ex.dayOfWeek(),
                            ex.startTime(), ex.endTime(), "Trùng phòng với lớp " + ex.classCode() + " (" + when + ")"));
                }
                if (teacherId != null && teacherId.equals(ex.teacherId())) {
                    result.add(new Conflict(ConflictType.TEACHER, ex.classId(), ex.classCode(), ex.className(), ex.dayOfWeek(),
                            ex.startTime(), ex.endTime(), "Trùng giáo viên với lớp " + ex.classCode() + " (" + when + ")"));
                }
            }
        }
        return result;
    }
}
