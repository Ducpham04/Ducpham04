package vn.educenter.catalog;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record CourseRequest(
            @NotBlank(message = "Vui lòng nhập mã khóa học") @Pattern(regexp = "^[A-Za-z0-9._-]{2,20}$", message = "Mã khóa học 2–20 ký tự, không dấu, không khoảng trắng") String code,
            @NotBlank(message = "Vui lòng nhập tên khóa học") @Size(max = 150) String name,
            @Size(max = 60) String level,
            @Min(value = 0, message = "Số buổi không hợp lệ") @Max(500) int totalSessions,
            @NotNull(message = "Vui lòng nhập học phí") @DecimalMin(value = "0", message = "Học phí không được âm") BigDecimal tuitionFee,
            @Size(max = 1000) String description,
            Boolean active) {}

    public record CourseResponse(Long id, String code, String name, String level, int totalSessions,
                                 BigDecimal tuitionFee, String description, boolean active) {}

    public record RoomRequest(
            @NotBlank(message = "Vui lòng nhập tên phòng") @Size(max = 60) String name,
            @Min(value = 1, message = "Sức chứa tối thiểu 1") @Max(500) int capacity,
            Boolean active) {}

    public record RoomResponse(Long id, String name, int capacity, boolean active) {}
}
