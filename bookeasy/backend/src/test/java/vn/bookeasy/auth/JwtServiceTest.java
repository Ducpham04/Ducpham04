package vn.bookeasy.auth;

import org.junit.jupiter.api.Test;
import vn.bookeasy.config.AppProperties;
import vn.bookeasy.config.TimeConfig;
import vn.bookeasy.support.MutableClock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private final MutableClock clock = new MutableClock(LocalDateTime.of(2030, 1, 7, 8, 0), TimeConfig.ZONE);

    private AppProperties props(String secret) {
        return new AppProperties(new AppProperties.Jwt(secret, 15, 7), List.of(), "", "", null, null, true, null);
    }

    @Test
    void roundTripsClaims() {
        JwtService jwt = new JwtService(props("0123456789abcdef0123456789abcdef!!"), clock);
        User u = new User();
        u.setId(5L);
        u.setEmail("a@b.vn");
        u.setRole(Role.STAFF);
        u.setBusinessId(2L);
        u.setStaffId(7L);
        AuthPrincipal p = jwt.parse(jwt.generateAccessToken(u)).orElseThrow();
        assertThat(p).isEqualTo(new AuthPrincipal(5L, "a@b.vn", Role.STAFF, 2L, 7L));
    }

    @Test
    void rejectsExpiredAndTamperedTokens() {
        JwtService jwt = new JwtService(props("0123456789abcdef0123456789abcdef!!"), clock);
        User u = new User();
        u.setId(1L);
        u.setEmail("x@y.vn");
        u.setRole(Role.OWNER);
        String token = jwt.generateAccessToken(u);
        assertThat(jwt.parse(token + "x")).isEmpty();
        JwtService other = new JwtService(props("another-secret-another-secret-123456"), clock);
        assertThat(other.parse(token)).isEmpty();
        clock.advance(Duration.ofMinutes(16));
        assertThat(jwt.parse(token)).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("short"), clock)).isInstanceOf(IllegalStateException.class);
    }
}
