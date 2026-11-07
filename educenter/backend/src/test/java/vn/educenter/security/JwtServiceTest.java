package vn.educenter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import vn.educenter.config.AppProperties;

class JwtServiceTest {

    private static AppProperties props(String secret) {
        return new AppProperties("Asia/Ho_Chi_Minh", java.util.List.of(), new AppProperties.Jwt(secret, 15, 7),
                null, null, null, null, null, new AppProperties.LoginRateLimit(5, 60));
    }

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T01:00:00Z"), ZoneOffset.UTC);

    @Test
    void roundTripsClaims() {
        JwtService jwt = new JwtService(props("a-very-long-test-secret-of-at-least-32-bytes"), clock);
        String token = jwt.generateAccessToken(new AuthUser(5L, 2L, Role.STUDENT, 9L, "ph@x.vn"));
        AuthUser u = jwt.parse(token).orElseThrow();
        assertThat(u).isEqualTo(new AuthUser(5L, 2L, Role.STUDENT, 9L, "ph@x.vn"));
    }

    @Test
    void rejectsExpiredTamperedAndForeignTokens() {
        JwtService jwt = new JwtService(props("a-very-long-test-secret-of-at-least-32-bytes"), clock);
        String token = jwt.generateAccessToken(new AuthUser(5L, 2L, Role.STAFF, null, "s@x.vn"));
        JwtService later = new JwtService(props("a-very-long-test-secret-of-at-least-32-bytes"), Clock.offset(clock, java.time.Duration.ofMinutes(16)));
        assertThat(later.parse(token)).isEmpty();
        assertThat(jwt.parse(token + "x")).isEmpty();
        JwtService other = new JwtService(props("another-very-long-secret-of-at-least-32-bytes"), clock);
        assertThat(other.parse(token)).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("short"), clock)).isInstanceOf(IllegalStateException.class);
    }
}
