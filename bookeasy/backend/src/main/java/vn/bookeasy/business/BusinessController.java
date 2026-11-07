package vn.bookeasy.business;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.business.BusinessDtos.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Cơ sở")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessService service;
    private final Clock clock;

    @GetMapping("/business")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public BusinessResponse get() {
        return service.get(CurrentUser.businessId());
    }

    @PutMapping("/business")
    @PreAuthorize("hasRole('OWNER')")
    public BusinessResponse update(@Valid @RequestBody UpdateBusinessRequest req) {
        return service.update(CurrentUser.businessId(), req);
    }

    @PostMapping(value = "/business/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public BusinessResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return service.uploadLogo(CurrentUser.businessId(), file);
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<BranchResponse> branches() {
        return service.listBranches(CurrentUser.businessId());
    }

    @PostMapping("/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public BranchResponse createBranch(@Valid @RequestBody BranchRequest req) {
        return service.createBranch(CurrentUser.businessId(), req);
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public BranchResponse updateBranch(@PathVariable Long id, @Valid @RequestBody BranchRequest req) {
        return service.updateBranch(CurrentUser.businessId(), id, req);
    }

    @PutMapping("/branches/{id}/hours")
    @PreAuthorize("hasRole('OWNER')")
    public BranchResponse updateHours(@PathVariable Long id, @Valid @RequestBody HoursRequest req) {
        return service.updateHours(CurrentUser.businessId(), id, req);
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<HolidayResponse> holidays(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return service.listHolidays(CurrentUser.businessId(), from == null ? LocalDate.now(clock).minusDays(30) : from);
    }

    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public HolidayResponse addHoliday(@Valid @RequestBody HolidayRequest req) {
        return service.addHoliday(CurrentUser.businessId(), req);
    }

    @DeleteMapping("/holidays/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deleteHoliday(@PathVariable Long id) {
        service.deleteHoliday(CurrentUser.businessId(), id);
    }
}
