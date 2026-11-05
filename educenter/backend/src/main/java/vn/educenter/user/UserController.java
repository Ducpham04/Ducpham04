package vn.educenter.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Nhân viên")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public List<UserDtos.UserResponse> list() {
        return service.listStaff();
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<UserDtos.TeacherOption> teachers() {
        return service.listTeachers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public UserDtos.UserResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public UserDtos.UserResponse enabled(@PathVariable Long id, @RequestBody UserDtos.EnabledRequest req) {
        return service.setEnabled(id, req.enabled());
    }
}
