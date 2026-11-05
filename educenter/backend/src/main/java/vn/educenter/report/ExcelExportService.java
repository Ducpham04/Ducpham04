package vn.educenter.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.attendance.AttendanceDtos;
import vn.educenter.attendance.AttendanceService;
import vn.educenter.billing.BillingDtos;
import vn.educenter.billing.BillingService;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportService reportService;
    private final BillingService billingService;
    private final AttendanceService attendanceService;

    @Transactional(readOnly = true)
    public byte[] revenue(int year) {
        List<ReportDtos.MonthValue> rows = reportService.revenue(year);
        List<ReportDtos.MonthCount> students = reportService.newStudents(year);
        return build("Doanh thu " + year, new String[]{"Tháng", "Doanh thu (₫)", "Học viên mới"}, (sheet, s) -> {
            int r = 1;
            BigDecimal sum = BigDecimal.ZERO;
            long sumStudents = 0;
            for (int i = 0; i < 12; i++) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue("Tháng " + (i + 1) + "/" + year);
                money(row.createCell(1), rows.get(i).value(), s.money);
                row.createCell(2).setCellValue(students.get(i).value());
                sum = sum.add(rows.get(i).value());
                sumStudents += students.get(i).value();
            }
            Row total = sheet.createRow(r);
            cell(total, 0, "Tổng cộng", s.bold);
            money(total.createCell(1), sum, s.boldMoney);
            Cell c = total.createCell(2);
            c.setCellValue(sumStudents);
            c.setCellStyle(s.bold);
        });
    }

    @Transactional(readOnly = true)
    public byte[] debts() {
        BillingDtos.DebtSummary summary = billingService.debts();
        return build("Công nợ", new String[]{"Mã HV", "Học viên", "SĐT", "SĐT phụ huynh", "Số hóa đơn", "Còn nợ (₫)", "Quá hạn (₫)", "Hạn sớm nhất"}, (sheet, s) -> {
            int r = 1;
            for (BillingDtos.DebtItem d : summary.items()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(d.studentCode());
                row.createCell(1).setCellValue(d.studentName());
                row.createCell(2).setCellValue(nz(d.phone()));
                row.createCell(3).setCellValue(nz(d.guardianPhone()));
                row.createCell(4).setCellValue(d.invoiceCount());
                money(row.createCell(5), d.outstanding(), s.money);
                money(row.createCell(6), d.overdueAmount(), s.money);
                row.createCell(7).setCellValue(d.oldestDueDate() == null ? "" : D.format(d.oldestDueDate()));
            }
            Row total = sheet.createRow(r);
            cell(total, 1, "Tổng cộng", s.bold);
            money(total.createCell(5), summary.totalOutstanding(), s.boldMoney);
            money(total.createCell(6), summary.totalOverdue(), s.boldMoney);
        });
    }

    @Transactional(readOnly = true)
    public byte[] attendance(LocalDate from, LocalDate to) {
        List<AttendanceDtos.ClassRate> rows = attendanceService.classRates(from, to);
        return build("Chuyên cần", new String[]{"Mã lớp", "Tên lớp", "Có mặt", "Đi muộn", "Vắng có phép", "Vắng không phép", "Tổng lượt", "Tỷ lệ (%)"}, (sheet, s) -> {
            Row info = sheet.createRow(1);
            info.createCell(0).setCellValue("Từ " + D.format(from) + " đến " + D.format(to));
            int r = 2;
            for (AttendanceDtos.ClassRate c : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(c.classCode());
                row.createCell(1).setCellValue(c.className());
                row.createCell(2).setCellValue(c.present());
                row.createCell(3).setCellValue(c.late());
                row.createCell(4).setCellValue(c.excused());
                row.createCell(5).setCellValue(c.absent());
                row.createCell(6).setCellValue(c.total());
                row.createCell(7).setCellValue(c.rate());
            }
        });
    }

    private interface SheetWriter {
        void write(Sheet sheet, Styles styles);
    }

    private record Styles(CellStyle header, CellStyle money, CellStyle bold, CellStyle boldMoney) {}

    private static byte[] build(String sheetName, String[] headers, SheetWriter writer) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            CellStyle header = wb.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            DataFormat fmt = wb.createDataFormat();
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(fmt.getFormat("#,##0"));
            CellStyle bold = wb.createCellStyle();
            bold.setFont(boldFont);
            CellStyle boldMoney = wb.createCellStyle();
            boldMoney.setFont(boldFont);
            boldMoney.setDataFormat(fmt.getFormat("#,##0"));
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) cell(h, i, headers[i], header);
            writer.write(sheet, new Styles(header, money, bold, boldMoney));
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 60 * 256));
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file Excel", e);
        }
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private static void money(Cell c, BigDecimal v, CellStyle style) {
        c.setCellValue(v == null ? 0 : v.doubleValue());
        c.setCellStyle(style);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
