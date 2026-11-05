package vn.starter.zma.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.auth.AuthDtos.TokenResponse;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;
import vn.starter.zma.security.JwtService;
import vn.starter.zma.user.*;
import vn.starter.zma.zalo.ZaloGraphClient;
import vn.starter.zma.zalo.ZaloProfile;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    /** Hash BCrypt giả để so sánh khi email không tồn tại – chống dò email qua thời gian phản hồi. */
    private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("dummy-password-for-timing");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ZaloGraphClient zaloGraphClient;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AppProperties props;
    private final Clock clock;

    /** Đăng nhập bằng access token Zalo: xác thực với Zalo, tạo mới hoặc ghép user theo zaloId. */
    @Transactional
    public TokenResponse loginWithZalo(String zaloAccessToken) {
        ZaloProfile profile = zaloGraphClient.getProfile(zaloAccessToken);
        User user = userRepository.findByZaloId(profile.id()).orElseGet(() -> {
            User u = new User();
            u.setZaloId(profile.id());
            u.setRole(Role.USER);
            u.setName(profile.name());
            return u;
        });
        // Cập nhật avatar mỗi lần đăng nhập; giữ tên nếu user đã tự đổi
        if (user.getName() == null || user.getName().isBlank()) user.setName(profile.name());
        if (profile.avatarUrl() != null) user.setAvatarUrl(profile.avatarUrl());
        ensureActive(user);
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse loginWithPassword(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        String hash = user == null || user.getPasswordHash() == null ? DUMMY_HASH : user.getPasswordHash();
        boolean ok = passwordEncoder.matches(password, hash);
        if (user == null || !ok || user.getRole() == Role.USER) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureActive(user);
        return issueTokens(user);
    }

    /** Đổi refresh token (rotation). Dùng lại token đã thu hồi → thu hồi toàn bộ phiên của user (phát hiện đánh cắp). */
    @Transactional(noRollbackFor = AppException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
        if (token.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {}", token.getUser().getId());
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        token.setRevokedAt(now);
        User user = token.getUser();
        ensureActive(user);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int n = refreshTokenRepository.deleteExpired(clock.instant());
        if (n > 0) log.info("Purged {} expired refresh tokens", n);
    }

    private TokenResponse issueTokens(User user) {
        Instant now = clock.instant();
        String raw = TokenHasher.newOpaqueToken();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(TokenHasher.sha256(raw));
        rt.setCreatedAt(now);
        rt.setExpiresAt(now.plus(props.jwt().refreshTtl()));
        refreshTokenRepository.save(rt);
        String access = jwtService.createAccessToken(user.getId(), user.getRole());
        return new TokenResponse(access, raw, jwtService.accessTtlSeconds(), "Bearer", userMapper.toDto(user));
    }

    private void ensureActive(User user) {
        if (user.getStatus() == UserStatus.LOCKED) throw new AppException(ErrorCode.ACCOUNT_LOCKED);
    }
}
