package vn.starter.zma.content;

import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ContentDtos {
    private ContentDtos() {}

    public record ArticleSummaryDto(Long id, String title, String slug, String category, String summary,
                                    String thumbnailUrl, boolean published, Instant publishedAt) {}

    public record ArticleDto(Long id, String title, String slug, String category, String summary, String content,
                             String thumbnailUrl, boolean published, Instant publishedAt, Instant createdAt, Instant updatedAt) {}

    public record ArticleRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 255) String title,
            @NotBlank(message = "Vui lòng chọn chuyên mục") @Size(max = 100) String category,
            @Size(max = 500) String summary,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 200000) String content,
            @Size(max = 500) @Pattern(regexp = "^(https?://|/).*|^$", message = "URL ảnh không hợp lệ") String thumbnailUrl,
            boolean published) {}

    public record ProductDto(Long id, String name, String description, long price, String imageUrl, int stock,
                             boolean active, boolean featured, Instant createdAt) {}

    public record ProductRequest(
            @NotBlank(message = "Vui lòng nhập tên sản phẩm") @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @PositiveOrZero(message = "Giá không hợp lệ") @Max(value = 1_000_000_000L, message = "Giá quá lớn") long price,
            @Size(max = 500) @Pattern(regexp = "^(https?://|/).*|^$", message = "URL ảnh không hợp lệ") String imageUrl,
            @PositiveOrZero(message = "Tồn kho không hợp lệ") int stock,
            boolean active,
            boolean featured) {}
}
