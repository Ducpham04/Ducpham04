package vn.educenter.classes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConflictDetectorTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static ConflictDetector.Slot slot(int d, String s, String e) {
        return new ConflictDetector.Slot(d, LocalTime.parse(s), LocalTime.parse(e));
    }
    private static ConflictDetector.ExistingSlot existing(long room, long teacher, int d, String s, String e, LocalDate from, LocalDate to) {
        return new ConflictDetector.ExistingSlot(99L, "IELTS-K1", "IELTS K1", room, teacher, from, to, d, LocalTime.parse(s), LocalTime.parse(e));
    }

    @Test
    void detectsRoomConflictWhenTimesOverlap() {
        var result = ConflictDetector.detect(1L, 5L, START, null, List.of(slot(1, "18:30", "20:00")),
                List.of(existing(1, 7, 1, "18:00", "19:30", START, null)));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(ConflictDetector.ConflictType.ROOM);
        assertThat(result.getFirst().message()).contains("IELTS-K1").contains("Thứ 2");
    }

    @Test
    void detectsTeacherConflictInDifferentRoom() {
        var result = ConflictDetector.detect(2L, 7L, START, null, List.of(slot(3, "19:00", "20:00")),
                List.of(existing(1, 7, 3, "18:00", "19:30", START, null)));
        assertThat(result).extracting(ConflictDetector.Conflict::type).containsExactly(ConflictDetector.ConflictType.TEACHER);
    }

    @Test
    void reportsBothRoomAndTeacher() {
        var result = ConflictDetector.detect(1L, 7L, START, null, List.of(slot(3, "19:00", "20:00")),
                List.of(existing(1, 7, 3, "18:00", "19:30", START, null)));
        assertThat(result).hasSize(2);
    }

    @Test
    void backToBackSlotsDoNotConflict() {
        var result = ConflictDetector.detect(1L, 7L, START, null, List.of(slot(1, "19:30", "21:00")),
                List.of(existing(1, 7, 1, "18:00", "19:30", START, null)));
        assertThat(result).isEmpty();
    }

    @Test
    void differentDayOrNonOverlappingDateRangeDoesNotConflict() {
        assertThat(ConflictDetector.detect(1L, 7L, START, null, List.of(slot(2, "18:00", "19:30")),
                List.of(existing(1, 7, 1, "18:00", "19:30", START, null)))).isEmpty();
        assertThat(ConflictDetector.detect(1L, 7L, START.plusMonths(3), null, List.of(slot(1, "18:00", "19:30")),
                List.of(existing(1, 7, 1, "18:00", "19:30", START.minusMonths(3), START.minusDays(1))))).isEmpty();
    }

    @Test
    void validatesOwnSlots() {
        assertThat(ConflictDetector.validateOwnSlots(List.of(slot(1, "19:00", "18:00")))).isNotEmpty();
        assertThat(ConflictDetector.validateOwnSlots(List.of(slot(1, "18:00", "19:30"), slot(1, "19:00", "20:00")))).isNotEmpty();
        assertThat(ConflictDetector.validateOwnSlots(List.of(slot(1, "18:00", "19:30"), slot(3, "18:00", "19:30")))).isEmpty();
    }
}
