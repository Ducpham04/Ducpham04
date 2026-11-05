package vn.bookeasy.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.bookeasy.business.BusinessCategory;

public final class AuthDtos {
    private AuthDtos() {
    }

    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Vui lòng nhập tên cơ sở") @Size(max = 150) String businessName,
            @NotBlank(message = "Vui lòng nhập đường dẫn")
            @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,58}[a-z0-9])$", message = "Chỉ dùng chữ thường không dấu, số và dấu gạch ngang (3–60 ký tự)")
            String slug,
            BusinessCategory category,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String fullName,
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Pattern(regexp = PASSWORD_RULE, message = "Mật khẩu tối thiểu 8 ký tự, gồm chữ và số")
            String password,
            @Size(max = 20) String phone) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, MeResponse user) {
    }

    public record MeResponse(Long id, String email, String fullName, Role role, Long businessId, Long staffId,
                             String businessName, String businessSlug) {
    }
}
