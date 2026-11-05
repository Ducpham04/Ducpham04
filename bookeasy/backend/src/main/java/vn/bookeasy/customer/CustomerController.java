package vn.bookeasy.customer;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.customer.CustomerDtos.*;

@Tag(name = "Khách hàng")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class CustomerController {
    private final CustomerService service;

    @GetMapping
    public PageResponse<CustomerSummary> search(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.search(CurrentUser.businessId(), q, page, size);
    }

    @GetMapping("/{id}")
    public CustomerDetail get(@PathVariable Long id) {
        return service.get(CurrentUser.businessId(), id);
    }

    @PatchMapping("/{id}")
    public CustomerDetail update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest req) {
        return service.update(CurrentUser.businessId(), id, req);
    }
}
