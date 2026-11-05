package vn.educenter.billing;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.educenter.common.PageResponse;
import vn.educenter.security.CurrentUser;

@Tag(name = "Học phí")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
public class BillingController {

    private final BillingService service;

    @GetMapping("/fee-plans")
    public List<BillingDtos.FeePlanResponse> feePlans() {
        return service.feePlans();
    }

    @PostMapping("/fee-plans")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDtos.FeePlanResponse createPlan(@Valid @RequestBody BillingDtos.FeePlanRequest req) {
        return service.createFeePlan(req);
    }

    @PutMapping("/fee-plans/{id}")
    public BillingDtos.FeePlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody BillingDtos.FeePlanRequest req) {
        return service.updateFeePlan(id, req);
    }

    @DeleteMapping("/fee-plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable Long id) {
        service.deleteFeePlan(id);
    }

    @GetMapping("/invoices")
    public PageResponse<BillingDtos.InvoiceResponse> invoices(@RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long studentId,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return service.searchInvoices(status, studentId, q, page, size);
    }

    @GetMapping("/students/{id}/invoices")
    public List<BillingDtos.InvoiceResponse> studentInvoices(@PathVariable Long id) {
        return service.invoicesOfStudent(CurrentUser.tenantId(), id);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDtos.InvoiceResponse create(@Valid @RequestBody BillingDtos.InvoiceRequest req) {
        return service.createInvoice(req);
    }

    @GetMapping("/invoices/{id}")
    public BillingDtos.InvoiceResponse get(@PathVariable Long id) {
        return service.getInvoice(id);
    }

    @PostMapping("/invoices/{id}/cancel")
    public BillingDtos.InvoiceResponse cancel(@PathVariable Long id, @Valid @RequestBody(required = false) BillingDtos.CancelRequest req) {
        return service.cancelInvoice(id, req == null ? null : req.reason());
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDtos.InvoiceResponse pay(@PathVariable Long id, @Valid @RequestBody BillingDtos.PaymentRequest req) {
        return service.recordPayment(id, req);
    }

    @GetMapping("/invoices/{id}/vietqr")
    public BillingDtos.VietQrResponse vietQr(@PathVariable Long id) {
        return service.vietQr(id);
    }

    @GetMapping("/debts")
    public BillingDtos.DebtSummary debts() {
        return service.debts();
    }

    @GetMapping("/payments/{id}/receipt.pdf")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        byte[] pdf = service.receiptPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(service.receiptFileName(id)).build().toString())
                .body(pdf);
    }

    @GetMapping("/banks")
    @PreAuthorize("isAuthenticated()")
    public java.util.Map<String, String> banks() {
        return VietQrService.BANKS;
    }
}
