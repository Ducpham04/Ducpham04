package vn.educenter.notification;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.educenter.common.BusinessException;
import vn.educenter.common.PageResponse;
import vn.educenter.security.CurrentUser;

@Tag(name = "Nhắc nhở")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTER_ADMIN')")
public class NotificationController {

    private final ReminderService reminderService;
    private final NotificationLogRepository logRepository;

    public record LogItem(Long id, NotificationLog.Channel channel, NotificationLog.Type type, String recipient, String subject,
                          NotificationLog.Status status, String error, Instant createdAt) {}

    @GetMapping("/logs")
    public PageResponse<LogItem> logs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(logRepository.findByTenantIdOrderByIdDesc(CurrentUser.tenantId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))),
                l -> new LogItem(l.getId(), l.getChannel(), l.getType(), l.getRecipient(), l.getSubject(), l.getStatus(), l.getError(), l.getCreatedAt()));
    }

    @PostMapping("/run/{kind}")
    public ReminderService.RunResult run(@PathVariable String kind) {
        Long tenantId = CurrentUser.tenantId();
        return switch (kind) {
            case "sessions" -> reminderService.runSessionReminders(tenantId);
            case "fees" -> reminderService.runFeeReminders(tenantId);
            default -> throw new BusinessException("Loại nhắc nhở không hợp lệ");
        };
    }
}
