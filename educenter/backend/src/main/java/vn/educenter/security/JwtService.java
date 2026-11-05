package vn.educenter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import vn.educenter.config.AppProperties;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET phải dài tối thiểu 32 byte");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTtlMinutes());
        this.clock = clock;
    }

    public String generateAccessToken(AuthUser user) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)));
        if (user.tenantId() != null) builder.claim("tid", user.tenantId());
        if (user.studentId() != null) builder.claim("sid", user.studentId());
        return builder.signWith(key).compact();
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(
                    Long.valueOf(c.getSubject()),
                    c.get("tid", Long.class),
                    Role.valueOf(c.get("role", String.class)),
                    c.get("sid", Long.class),
                    c.get("email", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
