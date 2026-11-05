package vn.starter.zma.feedback;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.feedback.FeedbackDtos.*;
import vn.starter.zma.security.CurrentUser;

@Tag(name = "Admin - Feedback")
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {
    private final FeedbackService service;

    @GetMapping
    public PageResponse<FeedbackSummaryDto> list(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) FeedbackStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(q, status, page, size);
    }

    @GetMapping("/{id}")
    public FeedbackDto get(@PathVariable Long id) {
        return service.get(id, CurrentUser.get());
    }

    @PatchMapping("/{id}")
    public FeedbackDto update(@PathVariable Long id, @Valid @RequestBody UpdateFeedbackRequest req) {
        return service.update(id, req);
    }
}
