package vn.bookeasy.report;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.CurrentUser;

import java.time.LocalDate;

@Tag(name = "Báo cáo")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class ReportController {
    private final ReportService service;

    @GetMapping("/summary")
    public ReportService.Summary summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(CurrentUser.businessId(), from, to);
    }
}
