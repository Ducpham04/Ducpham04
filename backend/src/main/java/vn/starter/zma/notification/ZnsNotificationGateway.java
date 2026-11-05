package vn.starter.zma.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.starter.zma.common.config.AppProperties;

import java.util.Map;
import java.util.UUID;

/**
 * Gửi ZNS (Zalo Notification Service) qua API: POST {zns}/message/template
 * Header: access_token = OA access token (hết hạn ~25h, cần cơ chế refresh – xem docs/PITFALLS.md).
 * Body: { phone: "84...", template_id, template_data: {...}, tracking_id }
 * Tài liệu: https://developers.zalo.me/docs/zalo-notification-service/
 */
@Slf4j
public class ZnsNotificationGateway implements NotificationGateway {
    private final RestClient rest;
    private final AppProperties.Notification cfg;

    public ZnsNotificationGateway(AppProperties.Notification cfg, RestClient.Builder builder) {
        this.cfg = cfg;
        this.rest = builder.baseUrl(cfg.znsBaseUrl() == null ? "https://business.openapi.zalo.me" : cfg.znsBaseUrl()).build();
    }

    @Override
    public NotificationResult send(NotificationMessage m) {
        if (m.channel() != NotificationMessage.Channel.ZNS) {
            return NotificationResult.skipped("Kênh OA chưa được bật trong ZnsNotificationGateway");
        }
        if (m.recipient() == null || m.templateId() == null || m.templateId().isBlank()) {
            return NotificationResult.skipped("Thiếu số điện thoại hoặc template_id");
        }
        if (cfg.oaAccessToken() == null || cfg.oaAccessToken().isBlank()) {
            return NotificationResult.failed("Chưa cấu hình ZALO_OA_ACCESS_TOKEN");
        }
        try {
            JsonNode res = rest.post().uri("/message/template")
                    .header("access_token", cfg.oaAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("phone", m.recipient(), "template_id", m.templateId(),
                            "template_data", m.data(), "tracking_id", UUID.randomUUID().toString()))
                    .retrieve().body(JsonNode.class);
            if (res != null && res.path("error").asInt(-1) == 0) {
                return NotificationResult.sent(res.path("data").path("msg_id").asText(null));
            }
            String err = res == null ? "empty response" : res.path("error").asText() + ": " + res.path("message").asText();
            log.warn("ZNS send failed: {}", err);
            return NotificationResult.failed(err);
        } catch (RestClientException e) {
            log.error("ZNS call error: {}", e.getMessage());
            return NotificationResult.failed(e.getMessage());
        }
    }
}
