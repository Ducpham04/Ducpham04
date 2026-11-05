package vn.bookeasy.booking;

import org.junit.jupiter.api.Test;
import vn.bookeasy.booking.SlotCalculator.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculatorTest {
    private static final LocalDate D = LocalDate.of(2030, 1, 8);
    private static final LocalDateTime EARLY = D.atStartOfDay();

    @Test
    void generatesStartsEvery15MinutesThatFitInWindow() {
        List<LocalTime> starts = SlotCalculator.availableStarts(D, List.of(new Window(LocalTime.of(9, 0), LocalTime.of(10, 30))),
                Set.of(), 60, EARLY);
        assertThat(starts).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 15), LocalTime.of(9, 30));
    }

    @Test
    void excludesStartsOverlappingBusyUnits() {
        Set<LocalDateTime> busy = Set.of(D.atTime(10, 0), D.atTime(10, 15));
        List<LocalTime> starts = SlotCalculator.availableStarts(D, List.of(new Window(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                busy, 60, EARLY);
        // 9:15..9:45 sẽ chạm 10:00; 10:00, 10:15 bận
        assertThat(starts).contains(LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(11, 0))
                .doesNotContain(LocalTime.of(9, 15), LocalTime.of(9, 45), LocalTime.of(10, 0), LocalTime.of(10, 15));
    }

    @Test
    void respectsEarliestBookableTime() {
        List<LocalTime> starts = SlotCalculator.availableStarts(D, List.of(new Window(LocalTime.of(9, 0), LocalTime.of(11, 0))),
                Set.of(), 30, D.atTime(10, 5));
        assertThat(starts).containsExactly(LocalTime.of(10, 15), LocalTime.of(10, 30));
    }

    @Test
    void alignsWindowStartUpToSlotUnit() {
        List<LocalTime> starts = SlotCalculator.availableStarts(D, List.of(new Window(LocalTime.of(9, 10), LocalTime.of(10, 0))),
                Set.of(), 30, EARLY);
        assertThat(starts).containsExactly(LocalTime.of(9, 15), LocalTime.of(9, 30));
    }

    @Test
    void mergesMultipleWindowsAndDeduplicates() {
        List<Window> windows = List.of(new Window(LocalTime.of(8, 0), LocalTime.of(9, 0)), new Window(LocalTime.of(14, 0), LocalTime.of(14, 30)));
        assertThat(SlotCalculator.availableStarts(D, windows, Set.of(), 30, EARLY))
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(8, 15), LocalTime.of(8, 30), LocalTime.of(14, 0));
    }

    @Test
    void windowIntersection() {
        Window shift = new Window(LocalTime.of(7, 0), LocalTime.of(12, 0));
        Window open = new Window(LocalTime.of(9, 0), LocalTime.of(21, 0));
        assertThat(shift.intersect(open)).isEqualTo(new Window(LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertThat(new Window(LocalTime.of(6, 0), LocalTime.of(8, 0)).intersect(open)).isNull();
    }

    @Test
    void unitsCoverDuration() {
        assertThat(SlotCalculator.units(D.atTime(9, 0), 45))
                .containsExactly(D.atTime(9, 0), D.atTime(9, 15), D.atTime(9, 30));
        assertThat(SlotCalculator.isAligned(D.atTime(9, 15))).isTrue();
        assertThat(SlotCalculator.isAligned(D.atTime(9, 10))).isFalse();
    }
}
