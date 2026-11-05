package vn.starter.zma.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.util.PageRequests;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationGateway gateway;
    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    /** Gửi và ghi nhật ký. Không bao giờ ném lỗi ra ngoài: thông báo thất bại không được làm hỏng nghiệp vụ chính. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResult send(NotificationMessage message) {
        NotificationResult result;
        try {
            result = gateway.send(message);
        } catch (RuntimeException e) {
            log.error("Notification gateway error", e);
            result = NotificationResult.failed(e.getMessage());
        }
        NotificationLog l = new NotificationLog();
        l.setUserId(message.userId());
        l.setChannel(message.channel().name());
        l.setTemplateId(message.templateId());
        l.setRecipient(message.recipient());
        l.setPayload(toJson(message));
        l.setStatus(result.status().name());
        l.setError(result.error() == null ? null : truncate(result.error()));
        l.setCreatedAt(Instant.now());
        logRepository.save(l);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationLogDto> list(int page, int size) {
        Page<NotificationLog> p = logRepository.findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(p, NotificationLogDto::from);
    }

    private String toJson(NotificationMessage m) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("data", m.data() == null ? java.util.Map.of() : m.data(),
                    "text", m.fallbackText() == null ? "" : m.fallbackText()));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncate(String s) {
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    public record NotificationLogDto(Long id, Long userId, String channel, String templateId, String recipient,
                                     String payload, String status, String error, Instant createdAt) {
        static NotificationLogDto from(NotificationLog l) {
            String r = l.getRecipient();
            String masked = r == null || r.length() < 6 ? r : r.substring(0, 4) + "****" + r.substring(r.length() - 2);
            return new NotificationLogDto(l.getId(), l.getUserId(), l.getChannel(), l.getTemplateId(), masked,
                    l.getPayload(), l.getStatus(), l.getError(), l.getCreatedAt());
        }
    }
}
