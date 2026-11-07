package vn.educenter.classes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionGeneratorTest {

    private final List<ConflictDetector.Slot> monWedFri = List.of(
            new ConflictDetector.Slot(1, LocalTime.of(18, 0), LocalTime.of(19, 30)),
            new ConflictDetector.Slot(3, LocalTime.of(18, 0), LocalTime.of(19, 30)),
            new ConflictDetector.Slot(5, LocalTime.of(18, 0), LocalTime.of(19, 30)));

    @Test
    void generatesThreeSessionsPerWeek() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        var result = SessionGenerator.generate(monday, monday.plusDays(13), monWedFri, Set.of());
        assertThat(result).hasSize(6);
        assertThat(result.getFirst().date()).isEqualTo(monday);
        assertThat(result).allMatch(o -> o.startTime().equals(LocalTime.of(18, 0)));
    }

    @Test
    void skipsExistingSessions() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        var result = SessionGenerator.generate(monday, monday.plusDays(6), monWedFri, Set.of(SessionGenerator.key(monday, LocalTime.of(18, 0))));
        assertThat(result).hasSize(2);
    }

    @Test
    void capsOpenEndedClassesAt180Days() {
        var result = SessionGenerator.generate(LocalDate.of(2026, 1, 5), null, monWedFri, Set.of());
        assertThat(result.getLast().date()).isBefore(LocalDate.of(2026, 1, 5).plusDays(SessionGenerator.MAX_DAYS));
        assertThat(result.size()).isBetween(75, 79);
    }
}
