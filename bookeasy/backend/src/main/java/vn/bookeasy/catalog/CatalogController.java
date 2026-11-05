package vn.bookeasy.catalog;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.catalog.CatalogDtos.*;

import java.util.List;

@Tag(name = "Dịch vụ")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService service;

    @GetMapping("/service-categories")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<CategoryResponse> categories() {
        return service.listCategories(CurrentUser.businessId());
    }

    @PostMapping("/service-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return service.createCategory(CurrentUser.businessId(), req);
    }

    @PutMapping("/service-categories/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return service.updateCategory(CurrentUser.businessId(), id, req);
    }

    @DeleteMapping("/service-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deleteCategory(@PathVariable Long id) {
        service.deleteCategory(CurrentUser.businessId(), id);
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public List<ServiceResponse> services() {
        return service.listServices(CurrentUser.businessId());
    }

    @GetMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public ServiceResponse getService(@PathVariable Long id) {
        return service.getService(CurrentUser.businessId(), id);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ServiceResponse createService(@Valid @RequestBody ServiceRequest req) {
        return service.createService(CurrentUser.businessId(), req);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest req) {
        return service.updateService(CurrentUser.businessId(), id, req);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deleteService(@PathVariable Long id) {
        service.deleteService(CurrentUser.businessId(), id);
    }
}
