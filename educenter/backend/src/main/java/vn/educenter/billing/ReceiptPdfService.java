package vn.educenter.billing;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.educenter.common.VnText;
import vn.educenter.config.AppProperties;
import vn.educenter.tenant.Tenant;

/** Phiếu thu A5 tiếng Việt có dấu (OpenPDF + DejaVu Sans nhúng). */
@Slf4j
@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Color INK = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LINE = new Color(226, 232, 240);

    private final BaseFont regular;
    private final BaseFont bold;

    public ReceiptPdfService(AppProperties props) {
        this.regular = loadFont(props.pdf() == null ? null : props.pdf().fontPath(), "fonts/DejaVuSans.ttf");
        this.bold = loadFont(null, "fonts/DejaVuSans-Bold.ttf");
    }

    public record ReceiptData(Tenant tenant, Invoice invoice, Payment payment, String studentCode, String studentName,
                              String receivedBy) {}

    public byte[] render(ReceiptData d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 36, 36, 30, 30);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Phiếu thu " + d.payment().getReceiptNo());
            doc.open();
            Font h = new Font(bold, 16, Font.NORMAL, INK);
            Font b = new Font(bold, 10, Font.NORMAL, INK);
            Font n = new Font(regular, 10, Font.NORMAL, INK);
            Font m = new Font(regular, 9, Font.NORMAL, MUTED);

            PdfPTable header = new PdfPTable(new float[]{3, 2});
            header.setWidthPercentage(100);
            PdfPCell left = cell();
            left.addElement(new Paragraph(d.tenant().getName(), b));
            if (d.tenant().getAddress() != null) left.addElement(new Paragraph(d.tenant().getAddress(), m));
            String contact = join(d.tenant().getPhone(), d.tenant().getEmail());
            if (!contact.isEmpty()) left.addElement(new Paragraph(contact, m));
            header.addCell(left);
            PdfPCell right = cell();
            Paragraph no = new Paragraph("Số: " + d.payment().getReceiptNo(), b);
            no.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(no);
            Paragraph inv = new Paragraph("Hóa đơn: " + d.invoice().getCode(), m);
            inv.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(inv);
            header.addCell(right);
            doc.add(header);

            Paragraph title = new Paragraph("PHIẾU THU HỌC PHÍ", h);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            doc.add(title);
            Paragraph when = new Paragraph(DATE_TIME.format(d.payment().getPaidAt()), m);
            when.setAlignment(Element.ALIGN_CENTER);
            when.setSpacingAfter(12);
            doc.add(when);

            PdfPTable body = new PdfPTable(new float[]{1.4f, 3.6f});
            body.setWidthPercentage(100);
            row(body, "Học viên", d.studentName() + " (" + d.studentCode() + ")", n, m);
            row(body, "Nội dung", d.invoice().getDescription(), n, m);
            row(body, "Hình thức", methodLabel(d.payment().getMethod()), n, m);
            row(body, "Số tiền", VnText.formatVnd(d.payment().getAmount()), new Font(bold, 12, Font.NORMAL, INK), m);
            row(body, "Bằng chữ", VnText.amountInWords(d.payment().getAmount().longValue()), n, m);
            row(body, "Tổng hóa đơn", VnText.formatVnd(d.invoice().getTotal()), n, m);
            row(body, "Đã thu (lũy kế)", VnText.formatVnd(d.invoice().getPaidAmount()), n, m);
            row(body, "Còn lại", VnText.formatVnd(d.invoice().remaining()), n, m);
            if (d.payment().getNote() != null) row(body, "Ghi chú", d.payment().getNote(), n, m);
            doc.add(body);

            PdfPTable sign = new PdfPTable(2);
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(14);
            sign.addCell(signCell("Người nộp tiền", "", b, m));
            sign.addCell(signCell("Người thu tiền", d.receivedBy() == null ? "" : d.receivedBy(), b, m));
            doc.add(sign);
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được phiếu thu", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    public static String methodLabel(Payment.Method method) {
        return switch (method) {
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER -> "Chuyển khoản";
            case CARD -> "Thẻ";
            case OTHER -> "Khác";
        };
    }

    private static void row(PdfPTable t, String label, String value, Font valueFont, Font labelFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "" : value, valueFont));
        for (PdfPCell c : new PdfPCell[]{l, v}) {
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(LINE);
            c.setPaddingTop(5);
            c.setPaddingBottom(6);
            t.addCell(c);
        }
    }

    private static PdfPCell signCell(String title, String name, Font b, Font m) {
        PdfPCell c = cell();
        Paragraph p1 = new Paragraph(title, b);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph("(Ký, ghi rõ họ tên)", m);
        p2.setAlignment(Element.ALIGN_CENTER);
        Paragraph p3 = new Paragraph(name, b);
        p3.setAlignment(Element.ALIGN_CENTER);
        p3.setSpacingBefore(34);
        c.addElement(p1);
        c.addElement(p2);
        c.addElement(p3);
        return c;
    }

    private static PdfPCell cell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private static String join(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isBlank()) sb.append(a);
        if (b != null && !b.isBlank()) sb.append(sb.isEmpty() ? "" : " · ").append(b);
        return sb.toString();
    }

    private static BaseFont loadFont(String configuredPath, String classpath) {
        try {
            if (configuredPath != null && !configuredPath.isBlank() && Files.exists(Path.of(configuredPath))) {
                return BaseFont.createFont(configuredPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            try (InputStream in = ReceiptPdfService.class.getClassLoader().getResourceAsStream(classpath)) {
                if (in == null) throw new IOException("Missing font " + classpath);
                byte[] bytes = in.readAllBytes();
                return BaseFont.createFont(classpath.substring(classpath.lastIndexOf('/') + 1), BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED, true, bytes, null);
            }
        } catch (IOException | DocumentException e) {
            log.warn("Không nạp được font tiếng Việt ({}), dùng Helvetica", e.getMessage());
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (IOException | DocumentException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
