package vn.starter.zma.notification;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.notification.NotificationService.NotificationLogDto;

@Tag(name = "Admin - Notifications")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationLogDto> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }
}
