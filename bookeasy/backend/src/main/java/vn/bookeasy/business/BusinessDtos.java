package vn.bookeasy.business;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class BusinessDtos {
    private BusinessDtos() {
    }

    public record BusinessResponse(Long id, String slug, String name, String description, String phone, String email,
                                   String address, String logoUrl, String brandColor, BusinessCategory category,
                                   BusinessStatus status, PaymentMode paymentMode, int depositPercent, String bankBin,
                                   String bankAccountNo, String bankAccountName, int minLeadMinutes,
                                   int bookingWindowDays, int cancelBeforeMinutes) {
    }

    public record UpdateBusinessRequest(
            @NotBlank(message = "Vui lòng nhập tên cơ sở") @Size(max = 150) String name,
            @Size(max = 1000) String description,
            @Size(max = 20) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 255) String address,
            @Size(max = 500) @Pattern(regexp = "^(https?://|/uploads/).*", message = "Logo phải là URL http(s)") String logoUrl,
            @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Màu phải ở dạng #RRGGBB") String brandColor,
            BusinessCategory category,
            @NotNull PaymentMode paymentMode,
            @Min(5) @Max(100) int depositPercent,
            @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Mã ngân hàng không hợp lệ") String bankBin,
            @Size(max = 30) @Pattern(regexp = "^[0-9]*$", message = "Số tài khoản chỉ gồm chữ số") String bankAccountNo,
            @Size(max = 100) String bankAccountName,
            @Min(0) @Max(10080) int minLeadMinutes,
            @Min(1) @Max(90) int bookingWindowDays,
            @Min(0) @Max(10080) int cancelBeforeMinutes) {
    }

    public record BranchHourDto(
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            boolean closed) {
    }

    public record BranchResponse(Long id, String name, String address, String phone, boolean active,
                                 List<BranchHourDto> hours) {
    }

    public record BranchRequest(
            @NotBlank(message = "Vui lòng nhập tên chi nhánh") @Size(max = 150) String name,
            @Size(max = 255) String address,
            @Size(max = 20) String phone,
            Boolean active) {
    }

    public record HoursRequest(@NotNull @Size(min = 7, max = 7, message = "Cần đủ 7 ngày") List<@Valid BranchHourDto> hours) {
    }

    public record HolidayRequest(
            @NotNull(message = "Vui lòng chọn ngày") LocalDate date,
            @NotBlank(message = "Vui lòng nhập tên ngày nghỉ") @Size(max = 150) String name,
            Long branchId) {
    }

    public record HolidayResponse(Long id, LocalDate date, String name, Long branchId) {
    }
}
