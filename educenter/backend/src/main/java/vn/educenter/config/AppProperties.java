package vn.educenter.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        List<String> corsOrigins,
        Jwt jwt,
        Seed seed,
        Mail mail,
        Reminders reminders,
        Zalo zalo,
        Pdf pdf,
        LoginRateLimit loginRateLimit) {

    public record Jwt(String secret, long accessTtlMinutes, long refreshTtlDays) {}

    public record Seed(boolean demo) {}

    public record Mail(String from, boolean enabled) {}

    public record Reminders(boolean enabled, String sessionCron, String feeCron, int feeDaysBefore) {}

    public record Zalo(String mode) {}

    public record Pdf(String fontPath) {}

    public record LoginRateLimit(int maxAttempts, long windowSeconds) {}
}
