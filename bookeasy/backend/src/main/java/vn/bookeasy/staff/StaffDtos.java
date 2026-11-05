package vn.bookeasy.staff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import vn.bookeasy.auth.AuthDtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class StaffDtos {
    private StaffDtos() {
    }

    public record StaffRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String fullName,
            @Size(max = 100) String title,
            @Size(max = 20) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 500) @Pattern(regexp = "^(https?://|/uploads/).*", message = "Ảnh phải là URL http(s)") String avatarUrl,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Màu phải ở dạng #RRGGBB") String color,
            @NotNull(message = "Vui lòng chọn chi nhánh") Long branchId,
            Boolean active,
            List<Long> serviceIds) {
    }

    public record StaffResponse(Long id, String fullName, String title, String phone, String email, String avatarUrl,
                                String color, Long branchId, boolean active, List<Long> serviceIds,
                                boolean hasAccount, String accountEmail) {
    }

    public record ShiftDto(@Min(1) @Max(7) int dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
    }

    public record ShiftsRequest(@NotNull List<@Valid ShiftDto> shifts) {
    }

    public record TimeOffRequest(@NotNull(message = "Chọn ngày bắt đầu") LocalDate startDate,
                                 @NotNull(message = "Chọn ngày kết thúc") LocalDate endDate,
                                 @Size(max = 255) String reason) {
    }

    public record TimeOffResponse(Long id, LocalDate startDate, LocalDate endDate, String reason) {
    }

    public record AccountRequest(
            @NotBlank @Email(message = "Email không hợp lệ") String email,
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số") String password) {
    }
}
