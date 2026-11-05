package vn.educenter.classes;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lớp học & lịch")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;
    private final SessionService sessionService;

    @GetMapping("/classes")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<ClassDtos.ClassListItem> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) ClassGroup.Status status,
                                              @RequestParam(required = false) Long teacherId) {
        return classService.list(q, status, teacherId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public ClassDtos.ClassDetail create(@Valid @RequestBody ClassDtos.ClassRequest req) {
        return classService.create(req);
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public ClassDtos.ClassDetail get(@PathVariable Long id) {
        return classService.get(id);
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public ClassDtos.ClassDetail update(@PathVariable Long id, @Valid @RequestBody ClassDtos.ClassRequest req) {
        return classService.update(id, req);
    }

    @PutMapping("/classes/{id}/schedules")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public ClassDtos.ClassDetail schedules(@PathVariable Long id, @Valid @RequestBody ClassDtos.SchedulesRequest req) {
        return classService.replaceSchedules(id, req);
    }

    @GetMapping("/classes/{id}/students")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<ClassDtos.ClassStudent> students(@PathVariable Long id) {
        return classService.students(id);
    }

    @PostMapping("/classes/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public ClassDtos.ClassStudent enroll(@PathVariable Long id, @Valid @RequestBody ClassDtos.EnrollRequest req) {
        return classService.enroll(id, req);
    }

    @PostMapping("/enrollments/{id}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public void endEnrollment(@PathVariable Long id, @RequestBody(required = false) ClassDtos.EndEnrollmentRequest req) {
        classService.endEnrollment(id, req == null ? null : req.status());
    }

    @GetMapping("/students/{id}/enrollments")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public List<ClassDtos.StudentEnrollment> studentEnrollments(@PathVariable Long id) {
        return classService.enrollmentsOfStudent(id);
    }

    @GetMapping("/classes/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<ClassDtos.SessionView> classSessions(@PathVariable Long id) {
        return sessionService.ofClass(id);
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<ClassDtos.SessionView> sessions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(required = false) Long teacherId,
                                                @RequestParam(required = false) Long roomId,
                                                @RequestParam(required = false) Long classId) {
        return sessionService.range(from, to, teacherId, roomId, classId);
    }

    @GetMapping("/sessions/today")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<ClassDtos.SessionView> today() {
        return sessionService.today();
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public ClassDtos.SessionView session(@PathVariable Long id) {
        return sessionService.get(id);
    }

    @PatchMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public ClassDtos.SessionView updateSession(@PathVariable Long id, @Valid @RequestBody ClassDtos.SessionUpdateRequest req) {
        return sessionService.update(id, req);
    }

    @PostMapping("/schedule/check-conflicts")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public List<ConflictDetector.Conflict> checkConflicts(@Valid @RequestBody ClassDtos.ConflictCheckRequest req) {
        return classService.checkConflicts(req);
    }
}
