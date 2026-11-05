package vn.starter.zma.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String publicBaseUrl,
        Jwt jwt,
        Cors cors,
        Zalo zalo,
        Notification notification,
        Payment payment,
        Storage storage,
        Seed seed) {

    public record Jwt(String secret, @DefaultValue("PT15M") Duration accessTtl,
                      @DefaultValue("P30D") Duration refreshTtl, @DefaultValue("zma-starter") String issuer) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Zalo(boolean mock, String appId, String appSecret,
                       @DefaultValue("https://graph.zalo.me") String graphBaseUrl,
                       @DefaultValue("PT10S") Duration timeout) {}

    public record Notification(@DefaultValue("mock") String provider, String znsBaseUrl,
                               String oaAccessToken, String feedbackTemplateId) {}

    public record Payment(@DefaultValue("mock") String provider, String checkoutPrivateKey, String method) {
        public boolean isMock() { return "mock".equalsIgnoreCase(provider); }
    }

    public record Storage(@DefaultValue("./data/uploads") String localDir, @DefaultValue("/uploads") String publicPath) {}

    public record Seed(@DefaultValue("true") boolean demoData) {}
}
