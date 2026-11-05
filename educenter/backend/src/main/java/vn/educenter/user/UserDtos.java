package vn.educenter.user;

import jakarta.validation.constraints.*;
import java.time.Instant;
import vn.educenter.security.Role;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserResponse(Long id, String email, String fullName, String phone, Role role, boolean enabled,
                               Instant lastLoginAt, Instant createdAt) {}

    public record CreateUserRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 120) String fullName,
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
            @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotNull(message = "Vui lòng chọn vai trò") Role role,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự") String password) {}

    public record UpdateUserRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 120) String fullName,
            @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotNull Role role,
            @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự") String password) {}

    public record EnabledRequest(boolean enabled) {}

    public record TeacherOption(Long id, String fullName) {}
}
