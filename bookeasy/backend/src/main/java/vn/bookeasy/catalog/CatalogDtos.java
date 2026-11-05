package vn.bookeasy.catalog;

import jakarta.validation.constraints.*;

import java.util.List;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record CategoryRequest(@NotBlank(message = "Vui lòng nhập tên danh mục") @Size(max = 100) String name,
                                  @Min(0) int sortOrder) {
    }

    public record CategoryResponse(Long id, String name, int sortOrder) {
    }

    public record ServiceRequest(
            @NotBlank(message = "Vui lòng nhập tên dịch vụ") @Size(max = 150) String name,
            Long categoryId,
            @Size(max = 1000) String description,
            @Min(value = 15, message = "Thời lượng tối thiểu 15 phút") @Max(value = 480, message = "Thời lượng tối đa 8 giờ")
            int durationMinutes,
            @Min(value = 0, message = "Giá không hợp lệ") @Max(value = 1_000_000_000L, message = "Giá không hợp lệ") long price,
            boolean active,
            @Min(0) int sortOrder,
            List<Long> staffIds) {
    }

    public record ServiceResponse(Long id, String name, Long categoryId, String description, int durationMinutes,
                                  long price, boolean active, int sortOrder, List<Long> staffIds) {
    }
}
