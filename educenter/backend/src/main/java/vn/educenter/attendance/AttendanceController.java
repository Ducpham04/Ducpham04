package vn.educenter.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Điểm danh")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @GetMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public AttendanceDtos.SessionAttendance get(@PathVariable Long id) {
        return service.forSession(id);
    }

    @PutMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public AttendanceDtos.SessionAttendance save(@PathVariable Long id, @Valid @RequestBody AttendanceDtos.SaveRequest req) {
        return service.save(id, req);
    }

    @GetMapping("/students/{id}/attendance")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public AttendanceDtos.StudentAttendance student(@PathVariable Long id) {
        return service.studentHistory(id);
    }

    @GetMapping("/reports/attendance/classes")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public List<AttendanceDtos.ClassRate> classRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.classRates(from, to);
    }
}
