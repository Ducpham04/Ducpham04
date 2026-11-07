package vn.educenter.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.common.BusinessException;
import vn.educenter.config.AppProperties;
import vn.educenter.security.AuthUser;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.JwtService;
import vn.educenter.security.LoginRateLimiter;
import vn.educenter.tenant.Tenant;
import vn.educenter.tenant.TenantRepository;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req, String clientIp) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String limitKey = clientIp + "|" + email;
        rateLimiter.check(limitKey);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(limitKey);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        ensureActive(user);
        rateLimiter.reset(limitKey);
        user.setLastLoginAt(clock.instant());
        return issueTokens(user);
    }

    /** Không rollback khi ném BusinessException để việc thu hồi phiên (phát hiện token bị dùng lại) được lưu. */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthDtos.TokenResponse refresh(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"));
        Instant now = clock.instant();
        if (token.getRevokedAt() != null) {
            // Token đã bị dùng lại sau khi xoay vòng → có thể bị đánh cắp: thu hồi toàn bộ phiên của user.
            refreshTokenRepository.revokeAllForUser(token.getUserId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn");
        }
        token.setRevokedAt(now);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"));
        ensureActive(user);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
            if (t.getRevokedAt() == null) t.setRevokedAt(clock.instant());
        });
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me() {
        User user = userRepository.findById(CurrentUser.get().userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
        return toMe(user);
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest req) {
        User user = userRepository.findById(CurrentUser.get().userId()).orElseThrow();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteExpired(clock.instant().minus(Duration.ofDays(1)));
    }

    private void ensureActive(User user) {
        if (!user.isEnabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên");
        }
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId()).orElseThrow();
            if (tenant.getStatus() != Tenant.Status.ACTIVE) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Trung tâm đang tạm khóa, vui lòng liên hệ EduCenter");
            }
        }
    }

    private AuthDtos.TokenResponse issueTokens(User user) {
        AuthUser principal = new AuthUser(user.getId(), user.getTenantId(), user.getRole(), user.getStudentId(), user.getEmail());
        String access = jwtService.generateAccessToken(principal);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(sha256(raw));
        token.setCreatedAt(clock.instant());
        token.setExpiresAt(clock.instant().plus(Duration.ofDays(props.jwt().refreshTtlDays())));
        refreshTokenRepository.save(token);
        return new AuthDtos.TokenResponse(access, raw, jwtService.accessTtlSeconds(), toMe(user));
    }

    private AuthDtos.MeResponse toMe(User user) {
        String tenantName = user.getTenantId() == null ? null
                : tenantRepository.findById(user.getTenantId()).map(Tenant::getName).orElse(null);
        return new AuthDtos.MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getTenantId(), tenantName, user.getStudentId());
    }

    static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
