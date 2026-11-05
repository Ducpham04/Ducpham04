package vn.bookeasy.booking;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class BookingDtos {
    private BookingDtos() {
    }

    public record PublicBookingRequest(
            @NotNull(message = "Vui lòng chọn chi nhánh") Long branchId,
            @NotNull(message = "Vui lòng chọn dịch vụ") Long serviceId,
            Long staffId,
            @NotNull(message = "Vui lòng chọn ngày") LocalDate date,
            @NotNull(message = "Vui lòng chọn giờ") LocalTime time,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên quá dài") String fullName,
            @NotBlank(message = "Vui lòng nhập số điện thoại") @Size(max = 20) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note) {
    }

    public record AdminBookingRequest(
            Long branchId,
            @NotNull(message = "Vui lòng chọn dịch vụ") Long serviceId,
            @NotNull(message = "Vui lòng chọn nhân viên") Long staffId,
            @NotNull(message = "Vui lòng chọn thời gian") LocalDateTime startAt,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String fullName,
            @NotBlank(message = "Vui lòng nhập số điện thoại") @Size(max = 20) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 500) String note,
            BookingSource source) {
    }

    public record RescheduleRequest(@NotNull LocalDateTime startAt, Long staffId) {
    }

    public record StatusRequest(@NotNull BookingStatus status, @Size(max = 255) String reason) {
    }

    public record NoteRequest(@Size(max = 1000) String internalNote) {
    }

    public record PublicCancelRequest(@NotBlank(message = "Vui lòng nhập số điện thoại") String phone,
                                      @Size(max = 255) String reason) {
    }

    public record PaymentInfo(String qrUrl, String bankBin, String accountNo, String accountName, long amount,
                              String content) {
    }

    public record PublicBookingResponse(String code, BookingStatus status, String businessName, String businessSlug,
                                        String branchName, String branchAddress, String serviceName,
                                        int durationMinutes, String staffName, LocalDateTime startAt,
                                        LocalDateTime endAt, long price, long amountDue, long amountPaid,
                                        PaymentStatus paymentStatus, LocalDateTime paymentExpiresAt,
                                        PaymentInfo payment, String customerName, boolean cancellable) {
    }

    public record Ref(Long id, String name) {
    }

    public record StaffRef(Long id, String name, String color) {
    }

    public record CustomerRef(Long id, String fullName, String phone, String email) {
    }

    public record ServiceRef(Long id, String name, int durationMinutes) {
    }

    public record BookingResponse(Long id, String code, BookingStatus status, BookingSource source,
                                  LocalDateTime startAt, LocalDateTime endAt, long price, long amountDue,
                                  long amountPaid, PaymentStatus paymentStatus, LocalDateTime paymentExpiresAt,
                                  CustomerRef customer, StaffRef staff, ServiceRef service, Ref branch,
                                  String customerNote, String internalNote, String cancelReason,
                                  LocalDateTime createdAt, PaymentInfo payment) {
    }
}
