package vn.educenter.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class TenantDtos {
    private TenantDtos() {
    }

    public record TenantResponse(Long id, String name, String slug, String phone, String email, String address,
                                 Tenant.Plan plan, Tenant.Status status, String bankCode, String bankAccountNo,
                                 String bankAccountName, Instant createdAt, Long studentCount, Long userCount) {}

    public record CenterRequest(
            @NotBlank(message = "Vui lòng nhập tên trung tâm") @Size(max = 150) String name,
            @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 255) String address,
            @Size(max = 20) String bankCode,
            @Pattern(regexp = "^$|^[0-9]{6,20}$", message = "Số tài khoản chỉ gồm 6–20 chữ số") String bankAccountNo,
            @Size(max = 100) String bankAccountName) {}

    public record OwnerRequest(
            @NotBlank(message = "Vui lòng nhập họ tên chủ trung tâm") @Size(max = 120) String fullName,
            @NotBlank @Email(message = "Email không hợp lệ") String email,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự") String password) {}

    public record CreateTenantRequest(
            @NotBlank(message = "Vui lòng nhập tên trung tâm") @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,60}$", message = "Mã trung tâm gồm chữ thường, số, gạch ngang (3–60 ký tự)") String slug,
            String phone,
            @Email String email,
            String address,
            @NotNull Tenant.Plan plan,
            @NotNull @Valid OwnerRequest owner) {}

    public record UpdateTenantRequest(
            @NotBlank @Size(max = 150) String name, String phone, @Email String email, String address,
            @NotNull Tenant.Plan plan) {}

    public record StatusRequest(@NotNull Tenant.Status status) {}
}
