package vn.starter.zma.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.feedback.FeedbackDtos.*;
import vn.starter.zma.security.CurrentUser;

import java.util.List;

@Tag(name = "Feedback")
@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService service;

    @Operation(summary = "Gửi phản ánh (multipart: part 'data' JSON + 'images' tối đa 5 ảnh)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDto create(@Valid @RequestPart("data") CreateFeedbackRequest data,
                              @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return service.create(CurrentUser.get().id(), data, images);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return FeedbackCategories.ALL;
    }

    @GetMapping("/me")
    public PageResponse<FeedbackSummaryDto> mine(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return service.mine(CurrentUser.get().id(), page, size);
    }

    @GetMapping("/{id}")
    public FeedbackDto get(@PathVariable Long id) {
        return service.get(id, CurrentUser.get());
    }
}
