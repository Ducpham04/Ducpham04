package vn.starter.zma.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockNotificationGateway implements NotificationGateway {
    @Override
    public NotificationResult send(NotificationMessage m) {
        log.info("[MOCK {}] to={} template={} data={} text={}", m.channel(), mask(m.recipient()), m.templateId(), m.data(), m.fallbackText());
        return NotificationResult.mocked();
    }

    private String mask(String r) {
        return r == null || r.length() < 6 ? r : r.substring(0, 4) + "****" + r.substring(r.length() - 2);
    }
}
