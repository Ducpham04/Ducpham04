package vn.educenter.student;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.educenter.common.PageResponse;

@Tag(name = "Học viên")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
public class StudentController {

    private final StudentService service;

    @GetMapping("/students")
    public PageResponse<StudentDtos.StudentListItem> search(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Student.Status status,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, page, size);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDtos.StudentResponse create(@Valid @RequestBody StudentDtos.StudentRequest req) {
        return service.create(req);
    }

    @GetMapping("/students/{id}")
    public StudentDtos.StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/students/{id}")
    public StudentDtos.StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentDtos.StudentRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/students/{id}/status")
    public StudentDtos.StudentResponse status(@PathVariable Long id, @Valid @RequestBody StudentDtos.StatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    @PostMapping("/students/{id}/portal-account")
    public StudentDtos.StudentResponse portalAccount(@PathVariable Long id, @Valid @RequestBody StudentDtos.PortalAccountRequest req) {
        return service.upsertPortalAccount(id, req);
    }

    @GetMapping("/students/{id}/guardians")
    public List<StudentDtos.GuardianResponse> guardians(@PathVariable Long id) {
        return service.guardians(id);
    }

    @PostMapping("/students/{id}/guardians")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDtos.GuardianResponse addGuardian(@PathVariable Long id, @Valid @RequestBody StudentDtos.GuardianRequest req) {
        return service.addGuardian(id, req);
    }

    @PutMapping("/guardians/{id}")
    public StudentDtos.GuardianResponse updateGuardian(@PathVariable Long id, @Valid @RequestBody StudentDtos.GuardianRequest req) {
        return service.updateGuardian(id, req);
    }

    @DeleteMapping("/guardians/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGuardian(@PathVariable Long id) {
        service.deleteGuardian(id);
    }
}
