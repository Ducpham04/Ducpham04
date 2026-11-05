package vn.educenter.student;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StudentDtos {
    private StudentDtos() {
    }

    public static final String PHONE_REGEX = "^$|^0\\d{9}$";
    public static final String PHONE_MSG = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0";

    public record StudentRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 120) String fullName,
            @Past(message = "Ngày sinh phải trước hôm nay") LocalDate dob,
            Student.Gender gender,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String phone,
            @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @Size(max = 150) String school,
            @Size(max = 60) String level,
            @Size(max = 255) String address,
            Student.Status status,
            LocalDate joinedAt,
            @Size(max = 1000) String note) {}

    public record StatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") Student.Status status) {}

    public record GuardianRequest(
            @NotBlank(message = "Vui lòng nhập họ tên phụ huynh") @Size(max = 120) String fullName,
            @Size(max = 30) String relationship,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String phone,
            @Email(message = "Email không hợp lệ") String email,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String zaloPhone,
            boolean primary) {}

    public record GuardianResponse(Long id, String fullName, String relationship, String phone, String email,
                                   String zaloPhone, boolean primary) {}

    public record StudentListItem(Long id, String code, String fullName, String phone, Student.Status status,
                                  LocalDate joinedAt, String primaryGuardian, String guardianPhone) {}

    public record StudentResponse(Long id, String code, String fullName, LocalDate dob, Student.Gender gender,
                                  String phone, String email, String school, String level, String address,
                                  Student.Status status, LocalDate joinedAt, String note, Instant createdAt,
                                  List<GuardianResponse> guardians, String portalEmail) {}

    public record PortalAccountRequest(
            @NotBlank @Email(message = "Email không hợp lệ") String email,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự") String password) {}
}
