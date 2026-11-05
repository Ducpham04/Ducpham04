package vn.educenter.billing;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {
    }

    public record FeePlanRequest(
            @NotBlank(message = "Vui lòng nhập tên gói") @Size(max = 150) String name,
            Long courseId,
            @NotNull(message = "Vui lòng nhập số tiền") @DecimalMin(value = "1000", message = "Số tiền tối thiểu 1.000 ₫") @Digits(integer = 13, fraction = 0) BigDecimal amount,
            @Min(0) @Max(500) Integer sessions,
            @Size(max = 500) String description,
            Boolean active) {}

    public record FeePlanResponse(Long id, String name, Long courseId, String courseName, BigDecimal amount,
                                  Integer sessions, String description, boolean active) {}

    public record InvoiceRequest(
            @NotNull(message = "Vui lòng chọn học viên") Long studentId,
            Long classId,
            Long feePlanId,
            @Size(max = 255) String description,
            @DecimalMin(value = "1000", message = "Số tiền tối thiểu 1.000 ₫") @Digits(integer = 13, fraction = 0) BigDecimal amount,
            @DecimalMin(value = "0", message = "Giảm giá không được âm") @Digits(integer = 13, fraction = 0) BigDecimal discount,
            @NotNull(message = "Vui lòng chọn hạn thanh toán") LocalDate dueDate,
            @Size(max = 500) String note) {}

    public record PaymentRequest(
            @NotNull(message = "Vui lòng nhập số tiền") @DecimalMin(value = "1000", message = "Số tiền tối thiểu 1.000 ₫") @Digits(integer = 13, fraction = 0) BigDecimal amount,
            @NotNull(message = "Vui lòng chọn hình thức") Payment.Method method,
            Instant paidAt,
            @Size(max = 255) String note) {}

    public record PaymentResponse(Long id, String receiptNo, BigDecimal amount, Payment.Method method, Instant paidAt,
                                  String note, String receivedByName) {}

    public record InvoiceResponse(Long id, String code, Long studentId, String studentCode, String studentName,
                                  Long classId, String classCode, Long feePlanId, String description,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal paidAmount,
                                  BigDecimal remaining, LocalDate dueDate, Invoice.Status status, boolean overdue,
                                  String note, Instant createdAt, List<PaymentResponse> payments) {}

    public record DebtItem(Long studentId, String studentCode, String studentName, String phone, String guardianPhone,
                           int invoiceCount, BigDecimal outstanding, BigDecimal overdueAmount, LocalDate oldestDueDate) {}

    public record DebtSummary(BigDecimal totalOutstanding, BigDecimal totalOverdue, int studentCount, List<DebtItem> items) {}

    public record VietQrResponse(String imageUrl, String bankCode, String bankName, String accountNo, String accountName,
                                 BigDecimal amount, String note) {}

    public record CancelRequest(@Size(max = 255) String reason) {}
}
