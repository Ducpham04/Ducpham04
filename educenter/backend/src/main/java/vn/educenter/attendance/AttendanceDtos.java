package vn.educenter.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.educenter.classes.ClassDtos;

public final class AttendanceDtos {
    private AttendanceDtos() {
    }

    public record RosterItem(Long studentId, String studentCode, String studentName, Attendance.Status status, String note) {}

    public record SessionAttendance(ClassDtos.SessionView session, List<RosterItem> items, boolean taken) {}

    public record MarkItem(@NotNull Long studentId, @NotNull(message = "Vui lòng chọn trạng thái") Attendance.Status status,
                           @Size(max = 255) String note) {}

    public record SaveRequest(@NotNull @Size(max = 300) List<@Valid MarkItem> items) {}

    public record ClassRate(Long classId, String classCode, String className, long present, long late, long excused,
                            long absent, long total, double rate) {}

    public record HistoryItem(Long sessionId, String classCode, String className, LocalDate date, LocalTime startTime,
                              Attendance.Status status, String note) {}

    public record StudentAttendance(long total, long attended, double rate, List<HistoryItem> items) {}
}
