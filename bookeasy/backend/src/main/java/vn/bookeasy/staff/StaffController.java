package vn.bookeasy.staff;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.staff.StaffDtos.*;

import java.util.List;

@Tag(name = "Nhân viên")
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<StaffResponse> list() {
        return service.list(CurrentUser.businessId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public StaffResponse get(@PathVariable Long id) {
        return service.get(CurrentUser.businessId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public StaffResponse create(@Valid @RequestBody StaffRequest req) {
        return service.create(CurrentUser.businessId(), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest req) {
        return service.update(CurrentUser.businessId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void delete(@PathVariable Long id) {
        service.deactivate(CurrentUser.businessId(), id);
    }

    @GetMapping("/{id}/shifts")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<ShiftDto> shifts(@PathVariable Long id) {
        return service.getShifts(CurrentUser.businessId(), id);
    }

    @PutMapping("/{id}/shifts")
    @PreAuthorize("hasRole('OWNER')")
    public List<ShiftDto> replaceShifts(@PathVariable Long id, @Valid @RequestBody ShiftsRequest req) {
        return service.replaceShifts(CurrentUser.businessId(), id, req);
    }

    @GetMapping("/{id}/time-off")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<TimeOffResponse> timeOff(@PathVariable Long id) {
        return service.listTimeOff(CurrentUser.businessId(), id);
    }

    @PostMapping("/{id}/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public TimeOffResponse addTimeOff(@PathVariable Long id, @Valid @RequestBody TimeOffRequest req) {
        return service.addTimeOff(CurrentUser.businessId(), id, req);
    }

    @DeleteMapping("/{id}/time-off/{offId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deleteTimeOff(@PathVariable Long id, @PathVariable Long offId) {
        service.deleteTimeOff(CurrentUser.businessId(), id, offId);
    }

    @PostMapping("/{id}/account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public StaffResponse createAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest req) {
        return service.createAccount(CurrentUser.businessId(), id, req);
    }
}
