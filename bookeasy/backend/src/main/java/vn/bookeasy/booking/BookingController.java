package vn.bookeasy.booking;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.CurrentUser;
import vn.bookeasy.booking.BookingDtos.*;
import vn.bookeasy.common.ApiException;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Lịch hẹn")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
public class BookingController {
    private final BookingService service;

    @GetMapping
    public List<BookingResponse> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                        @RequestParam(required = false) Long staffId,
                                        @RequestParam(required = false) Long branchId,
                                        @RequestParam(required = false) BookingStatus status,
                                        @RequestParam(required = false) String q) {
        if (!from.isBefore(to)) throw ApiException.badRequest("INVALID_RANGE", "Khoảng thời gian không hợp lệ");
        return service.search(CurrentUser.get(), from, to, staffId, branchId, status, q);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return service.get(CurrentUser.get(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody AdminBookingRequest req) {
        return service.createAdmin(CurrentUser.get(), req);
    }

    @PatchMapping("/{id}/reschedule")
    public BookingResponse reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest req) {
        return service.reschedule(CurrentUser.get(), id, req);
    }

    @PostMapping("/{id}/status")
    public BookingResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.changeStatus(CurrentUser.get(), id, req);
    }

    @PatchMapping("/{id}/note")
    public BookingResponse note(@PathVariable Long id, @Valid @RequestBody NoteRequest req) {
        return service.updateNote(CurrentUser.get(), id, req);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('OWNER')")
    public BookingResponse markPaid(@PathVariable Long id) {
        return service.markPaid(CurrentUser.get(), id);
    }
}
