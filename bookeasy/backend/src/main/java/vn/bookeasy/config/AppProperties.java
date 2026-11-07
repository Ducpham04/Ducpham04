package vn.bookeasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        List<String> corsOrigins,
        String publicUrl,
        String uploadDir,
        Payment payment,
        Mail mail,
        boolean rateLimitEnabled,
        Seed seed) {

    public record Jwt(String secret, long accessTtlMinutes, long refreshTtlDays) {
    }

    public record Payment(String webhookSecret, int holdMinutes, boolean mockEnabled) {
    }

    public record Mail(String from, boolean enabled) {
    }

    public record Seed(boolean demo) {
    }
}
