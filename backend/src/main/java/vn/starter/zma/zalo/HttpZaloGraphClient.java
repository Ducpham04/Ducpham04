package vn.starter.zma.zalo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * Gọi Zalo Graph API thật.
 * <p>Tài liệu: https://mini.zalo.me/documents/api/getAccessToken/ ,
 * https://mini.zalo.me/documents/api/getPhoneNumber/ , https://mini.zalo.me/documents/api/getLocation/</p>
 * <ul>
 *   <li>Hồ sơ: GET {graph}/v2.0/me?fields=id,name,picture — header {@code access_token}</li>
 *   <li>Giải mã SĐT / vị trí: GET {graph}/v2.0/me/info — header {@code access_token}, {@code code}, {@code secret_key}</li>
 * </ul>
 * Zalo trả HTTP 200 kèm {@code error != 0} khi lỗi, nên phải kiểm tra trường error.
 */
@Slf4j
public class HttpZaloGraphClient implements ZaloGraphClient {
    private final RestClient rest;
    private final AppProperties.Zalo cfg;

    public HttpZaloGraphClient(AppProperties.Zalo cfg, RestClient.Builder builder) {
        this.cfg = cfg;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(cfg.timeout());
        rf.setReadTimeout(cfg.timeout());
        this.rest = builder.baseUrl(cfg.graphBaseUrl()).requestFactory(rf).build();
    }

    @Override
    public ZaloProfile getProfile(String token) {
        if (token == null || token.isBlank()) throw new AppException(ErrorCode.ZALO_TOKEN_INVALID);
        JsonNode body = call(() -> rest.get().uri("/v2.0/me?fields=id,name,picture")
                .header("access_token", token).retrieve().body(JsonNode.class));
        if (body == null || body.path("error").asInt(0) != 0 || body.path("id").asText("").isEmpty()) {
            log.warn("Zalo /me rejected token: error={} message={}", body == null ? null : body.path("error").asInt(),
                    body == null ? null : body.path("message").asText());
            throw new AppException(ErrorCode.ZALO_TOKEN_INVALID);
        }
        String avatar = body.path("picture").path("data").path("url").asText(null);
        return new ZaloProfile(body.path("id").asText(), body.path("name").asText("Người dùng Zalo"), avatar);
    }

    @Override
    public String decodePhoneNumber(String userAccessToken, String phoneToken) {
        JsonNode data = decodeInfo(userAccessToken, phoneToken);
        String number = data.path("number").asText("");
        if (number.isEmpty()) throw new AppException(ErrorCode.ZALO_API_ERROR, "Không giải mã được số điện thoại");
        return number;
    }

    @Override
    public ZaloLocation decodeLocation(String userAccessToken, String locationToken) {
        JsonNode data = decodeInfo(userAccessToken, locationToken);
        String lat = data.path("latitude").asText("");
        String lng = data.path("longitude").asText("");
        if (lat.isEmpty() || lng.isEmpty()) throw new AppException(ErrorCode.ZALO_API_ERROR, "Không giải mã được vị trí");
        return new ZaloLocation(new BigDecimal(lat), new BigDecimal(lng));
    }

    private JsonNode decodeInfo(String userAccessToken, String code) {
        if (cfg.appSecret() == null || cfg.appSecret().isBlank()) {
            throw new AppException(ErrorCode.ZALO_API_ERROR, "Chưa cấu hình ZALO_APP_SECRET");
        }
        if (userAccessToken == null || code == null) throw new AppException(ErrorCode.BAD_REQUEST);
        JsonNode body = call(() -> rest.get().uri("/v2.0/me/info")
                .header("access_token", userAccessToken)
                .header("code", code)
                .header("secret_key", cfg.appSecret())
                .retrieve().body(JsonNode.class));
        if (body == null || body.path("error").asInt(0) != 0) {
            log.warn("Zalo /me/info error={} message={}", body == null ? null : body.path("error").asInt(),
                    body == null ? null : body.path("message").asText());
            throw new AppException(ErrorCode.ZALO_API_ERROR, "Zalo từ chối yêu cầu giải mã (token hết hạn hoặc sai secret)");
        }
        return body.path("data");
    }

    private JsonNode call(java.util.function.Supplier<JsonNode> s) {
        try {
            return s.get();
        } catch (RestClientException e) {
            log.error("Zalo Graph API call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.ZALO_API_ERROR);
        }
    }
}
