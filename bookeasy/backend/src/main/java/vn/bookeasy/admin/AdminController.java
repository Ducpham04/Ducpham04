package vn.bookeasy.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.Role;
import vn.bookeasy.auth.User;
import vn.bookeasy.auth.UserRepository;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.business.*;
import vn.bookeasy.common.ApiException;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Super admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final BusinessRepository businesses;
    private final UserRepository users;
    private final BookingRepository bookings;

    public record AdminBusiness(Long id, String slug, String name, BusinessCategory category, BusinessStatus status,
                                String ownerName, String ownerEmail, long bookingCount, LocalDateTime createdAt) {
    }

    public record StatusRequest(@NotNull BusinessStatus status) {
    }

    public record Stats(long businesses, long activeBusinesses, long bookings) {
    }

    @GetMapping("/businesses")
    @Transactional(readOnly = true)
    public List<AdminBusiness> list() {
        return businesses.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @PatchMapping("/businesses/{id}/status")
    @Transactional
    public AdminBusiness setStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        Business b = businesses.findById(id).orElseThrow(() -> ApiException.notFound("cơ sở"));
        b.setStatus(req.status());
        return toDto(b);
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Stats stats() {
        List<Business> all = businesses.findAll();
        return new Stats(all.size(), all.stream().filter(b -> b.getStatus() == BusinessStatus.ACTIVE).count(), bookings.count());
    }

    private AdminBusiness toDto(Business b) {
        User owner = users.findFirstByBusinessIdAndRoleOrderByIdAsc(b.getId(), Role.OWNER).orElse(null);
        return new AdminBusiness(b.getId(), b.getSlug(), b.getName(), b.getCategory(), b.getStatus(),
                owner == null ? null : owner.getFullName(), owner == null ? null : owner.getEmail(),
                bookings.countByBusinessId(b.getId()), b.getCreatedAt());
    }
}
