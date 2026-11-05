package vn.educenter.security;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import vn.educenter.common.BusinessException;
import vn.educenter.config.AppProperties;

/** Giới hạn số lần đăng nhập sai theo khóa (IP + email) trong cửa sổ thời gian. Đủ cho 1 instance. */
@Component
public class LoginRateLimiter {

    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;

    public LoginRateLimiter(AppProperties props, Clock clock) {
        this.maxAttempts = props.loginRateLimit().maxAttempts();
        this.windowMillis = props.loginRateLimit().windowSeconds() * 1000;
        this.clock = clock;
    }

    public void check(String key) {
        Deque<Long> q = failures.get(key);
        if (q == null) return;
        synchronized (q) {
            prune(q);
            if (q.size() >= maxAttempts) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn đã đăng nhập sai quá nhiều lần, vui lòng thử lại sau ít phút");
            }
        }
    }

    public void recordFailure(String key) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(clock.millis());
        }
        if (failures.size() > 10_000) failures.clear();
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> q) {
        long cutoff = clock.millis() - windowMillis;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
    }
}
