package vn.educenter.classes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.educenter.student.Student;

public final class ClassDtos {
    private ClassDtos() {
    }

    public record SlotDto(
            @Min(value = 1, message = "Thứ không hợp lệ") @Max(value = 7, message = "Thứ không hợp lệ") int dayOfWeek,
            @NotNull(message = "Vui lòng nhập giờ bắt đầu") LocalTime startTime,
            @NotNull(message = "Vui lòng nhập giờ kết thúc") LocalTime endTime) {

        public ConflictDetector.Slot toSlot() {
            return new ConflictDetector.Slot(dayOfWeek, startTime.withSecond(0).withNano(0), endTime.withSecond(0).withNano(0));
        }
    }

    public record ClassRequest(
            @NotNull(message = "Vui lòng chọn khóa học") Long courseId,
            @NotBlank(message = "Vui lòng nhập mã lớp") @Pattern(regexp = "^[A-Za-z0-9._-]{2,30}$", message = "Mã lớp 2–30 ký tự, không dấu, không khoảng trắng") String code,
            @NotBlank(message = "Vui lòng nhập tên lớp") @Size(max = 150) String name,
            Long teacherId,
            Long roomId,
            @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0") @Max(200) int maxStudents,
            @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDate startDate,
            LocalDate endDate,
            ClassGroup.Status status,
            @Size(max = 1000) String note) {

        @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
        public boolean isDateRangeValid() {
            return endDate == null || startDate == null || !endDate.isBefore(startDate);
        }
    }

    public record SchedulesRequest(@NotNull @Size(max = 14, message = "Tối đa 14 khung giờ/tuần") List<@Valid SlotDto> slots) {}

    public record ClassListItem(Long id, String code, String name, Long courseId, String courseName, Long teacherId,
                                String teacherName, Long roomId, String roomName, int maxStudents, long enrolledCount,
                                LocalDate startDate, LocalDate endDate, ClassGroup.Status status, List<SlotDto> schedules) {}

    public record ClassDetail(Long id, String code, String name, Long courseId, String courseName, Long teacherId,
                              String teacherName, Long roomId, String roomName, int maxStudents, long enrolledCount,
                              LocalDate startDate, LocalDate endDate, ClassGroup.Status status, String note,
                              List<SlotDto> schedules, long totalSessions, long completedSessions) {}

    public record EnrollRequest(@NotNull(message = "Vui lòng chọn học viên") Long studentId, LocalDate startDate) {}

    public record EndEnrollmentRequest(Enrollment.Status status) {}

    public record ClassStudent(Long enrollmentId, Long studentId, String studentCode, String studentName, String phone,
                               Student.Status studentStatus, LocalDate startDate, LocalDate endDate,
                               Enrollment.Status status, Double attendanceRate) {}

    public record StudentEnrollment(Long enrollmentId, Long classId, String classCode, String className,
                                    String courseName, String teacherName, LocalDate startDate, LocalDate endDate,
                                    Enrollment.Status status, ClassGroup.Status classStatus) {}

    public record SessionView(Long id, Long classId, String classCode, String className, LocalDate date,
                              LocalTime startTime, LocalTime endTime, Long roomId, String roomName, Long teacherId,
                              String teacherName, ClassSession.Status status, String note, long attendanceCount,
                              long studentCount) {}

    public record SessionUpdateRequest(
            LocalDate date, LocalTime startTime, LocalTime endTime, Long roomId, Long teacherId,
            ClassSession.Status status, @Size(max = 500) String note) {}

    public record ConflictCheckRequest(
            Long classId, Long roomId, Long teacherId,
            @NotNull LocalDate startDate, LocalDate endDate,
            @NotNull List<@Valid SlotDto> slots) {}
}
