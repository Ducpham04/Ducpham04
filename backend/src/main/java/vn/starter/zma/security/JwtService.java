package vn.starter.zma.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;
import vn.starter.zma.user.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final AppProperties.Jwt cfg;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        this.cfg = props.jwt();
        byte[] bytes = cfg.secret() == null ? new byte[0] : cfg.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài tối thiểu 32 byte (APP_JWT_SECRET)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.clock = clock;
    }

    public String createAccessToken(Long userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(cfg.issuer())
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(cfg.accessTtl())))
                .signWith(key)
                .compact();
    }

    public long accessTtlSeconds() {
        return cfg.accessTtl().toSeconds();
    }

    public AuthUser parseAccessToken(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(cfg.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build().parseSignedClaims(token).getPayload();
            if (!"access".equals(c.get("typ", String.class))) {
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }
            return new AuthUser(Long.valueOf(c.getSubject()), Role.valueOf(c.get("role", String.class)));
        } catch (ExpiredJwtException e) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }
}
