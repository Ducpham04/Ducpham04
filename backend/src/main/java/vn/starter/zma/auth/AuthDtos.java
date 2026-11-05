package vn.starter.zma.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.starter.zma.user.UserDto;

public final class AuthDtos {
    private AuthDtos() {}

    public record ZaloLoginRequest(@NotBlank(message = "Thiếu access token Zalo") @Size(max = 4096) String accessToken) {}

    public record LoginRequest(@NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 190) String email,
                               @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(max = 100) String password) {}

    public record RefreshRequest(@NotBlank(message = "Thiếu refresh token") @Size(max = 200) String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType, UserDto user) {}
}
