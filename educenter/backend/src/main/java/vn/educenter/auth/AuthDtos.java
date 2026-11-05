package vn.educenter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.educenter.security.Role;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(max = 72) String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu mới từ 8 đến 72 ký tự") String newPassword) {}

    public record MeResponse(Long id, String email, String fullName, Role role, Long tenantId, String tenantName,
                             Long studentId) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, MeResponse user) {}
}
