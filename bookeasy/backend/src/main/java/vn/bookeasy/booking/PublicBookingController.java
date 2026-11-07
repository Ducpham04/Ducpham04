package vn.bookeasy.booking;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.booking.BookingDtos.*;
import vn.bookeasy.booking.PublicDtos.*;
import vn.bookeasy.business.*;
import vn.bookeasy.catalog.ServiceCategoryRepository;
import vn.bookeasy.catalog.ServiceItem;
import vn.bookeasy.catalog.ServiceItemRepository;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.ClientIp;
import vn.bookeasy.common.RateLimiter;
import vn.bookeasy.staff.Staff;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Đặt lịch công khai")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBookingController {
    private final BookingService bookingService;
    private final AvailabilityService availability;
    private final BranchRepository branches;
    private final ServiceCategoryRepository categories;
    private final ServiceItemRepository services;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    @GetMapping("/businesses/{slug}")
    @Transactional(readOnly = true)
    public PublicBusiness business(@PathVariable String slug) {
        Business b = bookingService.requireActiveBusiness(slug);
        List<ServiceItem> svcs = services.findActiveWithStaff(b.getId()).stream()
                .filter(s -> s.getStaff().stream().anyMatch(Staff::isActive)).toList();
        return new PublicBusiness(b.getSlug(), b.getName(), b.getDescription(), b.getPhone(), b.getAddress(),
                b.getLogoUrl(), b.getBrandColor(), b.getCategory(),
                b.paymentConfigured() ? b.getPaymentMode() : PaymentMode.NONE, b.getDepositPercent(),
                b.getBookingWindowDays(),
                branches.findByBusinessIdAndActiveTrueOrderByIdAsc(b.getId()).stream()
                        .map(br -> new PublicBranch(br.getId(), br.getName(), br.getAddress(), br.getPhone())).toList(),
                categories.findByBusinessIdOrderBySortOrderAscIdAsc(b.getId()).stream()
                        .map(c -> new PublicCategory(c.getId(), c.getName())).toList(),
                svcs.stream().map(s -> new PublicService(s.getId(), s.getName(), s.getDescription(),
                        s.getDurationMinutes(), s.getPrice(), s.getCategoryId())).toList());
    }

    @GetMapping("/businesses/{slug}/staff")
    @Transactional(readOnly = true)
    public List<PublicStaff> staff(@PathVariable String slug, @RequestParam Long serviceId, @RequestParam Long branchId) {
        Business b = bookingService.requireActiveBusiness(slug);
        ServiceItem svc = activeService(b, serviceId);
        return availability.candidates(svc, branchId, null).stream()
                .map(s -> new PublicStaff(s.getId(), s.getFullName(), s.getTitle(), s.getAvatarUrl(), s.getColor())).toList();
    }

    @GetMapping("/businesses/{slug}/availability")
    public List<AvailabilityService.SlotOption> availability(@PathVariable String slug, @RequestParam Long serviceId,
                                                             @RequestParam Long branchId,
                                                             @RequestParam(required = false) Long staffId,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Business b = bookingService.requireActiveBusiness(slug);
        return availability.slotsForDay(b, branchId, activeService(b, serviceId), staffId, date);
    }

    @GetMapping("/businesses/{slug}/availability/days")
    public List<AvailabilityService.DayAvailability> days(@PathVariable String slug, @RequestParam Long serviceId,
                                                          @RequestParam Long branchId,
                                                          @RequestParam(required = false) Long staffId,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(defaultValue = "14") int days) {
        Business b = bookingService.requireActiveBusiness(slug);
        return availability.days(b, branchId, activeService(b, serviceId), staffId,
                from == null ? LocalDate.now(clock) : from, Math.min(Math.max(days, 1), 31));
    }

    @PostMapping("/businesses/{slug}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicBookingResponse create(@PathVariable String slug, @Valid @RequestBody PublicBookingRequest req,
                                        HttpServletRequest http) {
        rateLimiter.check("book:" + ClientIp.of(http), 10, 6);
        return bookingService.createPublic(slug, req);
    }

    @GetMapping("/bookings/{code}")
    public PublicBookingResponse lookup(@PathVariable String code, @RequestParam String phone, HttpServletRequest http) {
        rateLimiter.check("lookup:" + ClientIp.of(http), 30, 20);
        return bookingService.getPublic(code, phone);
    }

    @PostMapping("/bookings/{code}/cancel")
    public PublicBookingResponse cancel(@PathVariable String code, @Valid @RequestBody PublicCancelRequest req,
                                        HttpServletRequest http) {
        rateLimiter.check("lookup:" + ClientIp.of(http), 30, 20);
        return bookingService.cancelPublic(code, req);
    }

    private ServiceItem activeService(Business b, Long serviceId) {
        return services.findByIdAndBusinessId(serviceId, b.getId()).filter(ServiceItem::isActive)
                .orElseThrow(() -> ApiException.notFound("dịch vụ"));
    }
}
