package vn.educenter.tenant;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Trung tâm")
@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService service;

    @GetMapping("/api/v1/admin/tenants")
    public List<TenantDtos.TenantResponse> list() {
        return service.listAll();
    }

    @PostMapping("/api/v1/admin/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDtos.TenantResponse create(@Valid @RequestBody TenantDtos.CreateTenantRequest req) {
        return service.create(req);
    }

    @PutMapping("/api/v1/admin/tenants/{id}")
    public TenantDtos.TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantDtos.UpdateTenantRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/api/v1/admin/tenants/{id}/status")
    public TenantDtos.TenantResponse status(@PathVariable Long id, @Valid @RequestBody TenantDtos.StatusRequest req) {
        return service.setStatus(id, req.status());
    }

    @GetMapping("/api/v1/center")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public TenantDtos.TenantResponse center() {
        return service.currentCenter();
    }

    @PutMapping("/api/v1/center")
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public TenantDtos.TenantResponse updateCenter(@Valid @RequestBody TenantDtos.CenterRequest req) {
        return service.updateCurrentCenter(req);
    }
}
