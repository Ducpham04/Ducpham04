package vn.starter.zma.feedback;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    public record CreateFeedbackRequest(
            @NotBlank(message = "Vui lòng chọn chuyên mục") @Size(max = 100) String category,
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(min = 5, max = 150, message = "Tiêu đề từ 5–150 ký tự") String title,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(min = 10, max = 2000, message = "Nội dung từ 10–2000 ký tự") String content,
            @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự") String address,
            @DecimalMin(value = "-90") @DecimalMax(value = "90") BigDecimal lat,
            @DecimalMin(value = "-180") @DecimalMax(value = "180") BigDecimal lng) {}

    public record UpdateFeedbackRequest(
            @NotNull(message = "Vui lòng chọn trạng thái") FeedbackStatus status,
            @Size(max = 2000, message = "Phản hồi tối đa 2000 ký tự") String adminReply) {}

    public record FeedbackSummaryDto(Long id, String code, String category, String title, FeedbackStatus status,
                                     Instant createdAt, Instant updatedAt, String userName) {}

    public record FeedbackDto(Long id, String code, String category, String title, String content, String address,
                              BigDecimal lat, BigDecimal lng, FeedbackStatus status, String adminReply,
                              List<String> images, Instant createdAt, Instant updatedAt,
                              Long userId, String userName, String userPhone) {}
}
