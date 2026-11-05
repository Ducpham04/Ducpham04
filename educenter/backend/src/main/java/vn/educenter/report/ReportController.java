package vn.educenter.report;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.educenter.common.BusinessException;

@Tag(name = "Báo cáo")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
public class ReportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;
    private final ExcelExportService excel;
    private final Clock clock;

    @GetMapping("/dashboard")
    public ReportDtos.Dashboard dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/revenue")
    public List<ReportDtos.MonthValue> revenue(@RequestParam(required = false) Integer year) {
        return reportService.revenue(year(year));
    }

    @GetMapping("/new-students")
    public List<ReportDtos.MonthCount> newStudents(@RequestParam(required = false) Integer year) {
        return reportService.newStudents(year(year));
    }

    @GetMapping("/export/revenue.xlsx")
    public ResponseEntity<byte[]> exportRevenue(@RequestParam(required = false) Integer year) {
        int y = year(year);
        return file(excel.revenue(y), "doanh-thu-" + y + ".xlsx");
    }

    @GetMapping("/export/debts.xlsx")
    public ResponseEntity<byte[]> exportDebts() {
        return file(excel.debts(), "cong-no-" + LocalDate.now(clock) + ".xlsx");
    }

    @GetMapping("/export/attendance.xlsx")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate t = to == null ? LocalDate.now(clock) : to;
        LocalDate f = from == null ? t.minusDays(29) : from;
        if (f.isAfter(t)) throw new BusinessException("Khoảng ngày không hợp lệ");
        return file(excel.attendance(f, t), "chuyen-can-" + f + "_" + t + ".xlsx");
    }

    private int year(Integer year) {
        int y = year == null ? LocalDate.now(clock).getYear() : year;
        if (y < 2000 || y > 2100) throw new BusinessException("Năm không hợp lệ");
        return y;
    }

    private static ResponseEntity<byte[]> file(byte[] data, String name) {
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(data);
    }
}
