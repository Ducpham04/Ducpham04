package vn.bookeasy.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Token bucket đơn giản trong bộ nhớ, theo khóa (ví dụ "login:" + IP). */
@Component
public class RateLimiter {
    private record Bucket(double tokens, long lastRefillNanos) {
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public RateLimiter(vn.bookeasy.config.AppProperties props) {
        this.enabled = props.rateLimitEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param capacity      số yêu cầu tối đa trong một cửa sổ
     * @param refillPerMin  số token hồi lại mỗi phút
     */
    public void check(String key, int capacity, int refillPerMin) {
        if (!enabled) return;
        long now = System.nanoTime();
        Bucket updated = buckets.compute(key, (k, b) -> {
            if (b == null) return new Bucket(capacity - 1, now);
            double refill = (now - b.lastRefillNanos()) / 60_000_000_000d * refillPerMin;
            double tokens = Math.min(capacity, b.tokens() + refill);
            return new Bucket(tokens - 1, now);
        });
        if (updated.tokens() < 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút");
        }
        if (buckets.size() > 50_000) buckets.clear();
    }
}
