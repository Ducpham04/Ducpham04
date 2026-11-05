package vn.educenter.portal;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.educenter.attendance.AttendanceDtos;
import vn.educenter.billing.BillingDtos;
import vn.educenter.classes.ClassDtos;

@Tag(name = "Cổng phụ huynh / học viên")
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService service;

    @GetMapping("/me")
    public PortalService.PortalMe me() {
        return service.me();
    }

    @GetMapping("/sessions")
    public List<ClassDtos.SessionView> sessions() {
        return service.sessions();
    }

    @GetMapping("/attendance")
    public AttendanceDtos.StudentAttendance attendance() {
        return service.attendance();
    }

    @GetMapping("/invoices")
    public List<BillingDtos.InvoiceResponse> invoices() {
        return service.invoices();
    }

    @GetMapping("/invoices/{id}/vietqr")
    public BillingDtos.VietQrResponse vietQr(@PathVariable Long id) {
        return service.vietQr(id);
    }
}
