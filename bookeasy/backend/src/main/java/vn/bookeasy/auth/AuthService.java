package vn.bookeasy.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.auth.AuthDtos.*;
import vn.bookeasy.business.*;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.config.AppProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Set<String> RESERVED_SLUGS = Set.of("admin", "api", "app", "login", "register", "b", "www", "static", "uploads");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final BusinessRepository businesses;
    private final BusinessSetupService businessSetup;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String slug = req.slug().toLowerCase(Locale.ROOT);
        if (RESERVED_SLUGS.contains(slug) || businesses.existsBySlug(slug)) {
            throw ApiException.conflict("SLUG_TAKEN", "Đường dẫn này đã được sử dụng, vui lòng chọn đường dẫn khác");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email đã được đăng ký");
        }
        Business b = businessSetup.createBusiness(req.businessName().trim(), slug,
                req.category() == null ? BusinessCategory.SPA : req.category(), TextUtils.blankToNull(req.phone()), email);
        User owner = new User();
        owner.setBusinessId(b.getId());
        owner.setEmail(email);
        owner.setFullName(req.fullName().trim());
        owner.setPasswordHash(passwordEncoder.encode(req.password()));
        owner.setRole(Role.OWNER);
        users.save(owner);
        return issueTokens(owner);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));
        ensureCanLogin(user);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Phiên đăng nhập không hợp lệ"));
        if (token.getRevokedAt() != null) {
            // Token đã thu hồi bị dùng lại → có thể bị đánh cắp: thu hồi mọi phiên của user.
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            throw ApiException.unauthorized("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }
        token.setRevokedAt(now);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Phiên đăng nhập không hợp lệ"));
        ensureCanLogin(user);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null) return;
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            if (t.getRevokedAt() == null) t.setRevokedAt(LocalDateTime.now(clock));
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Vui lòng đăng nhập"));
        return toMe(u);
    }

    private void ensureCanLogin(User user) {
        if (!user.isActive()) throw ApiException.forbidden("Tài khoản đã bị vô hiệu hóa");
        if (user.getBusinessId() != null) {
            Business b = businesses.findById(user.getBusinessId()).orElseThrow();
            if (b.getStatus() == BusinessStatus.SUSPENDED) {
                throw ApiException.forbidden("Cơ sở đang bị tạm khóa, vui lòng liên hệ BookEasy");
            }
        }
    }

    private TokenResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(hash(raw));
        rt.setCreatedAt(now);
        rt.setExpiresAt(now.plusDays(props.jwt().refreshTtlDays()));
        refreshTokens.save(rt);
        return new TokenResponse(jwtService.generateAccessToken(user), raw, jwtService.accessTtlSeconds(), toMe(user));
    }

    private MeResponse toMe(User u) {
        Business b = u.getBusinessId() == null ? null : businesses.findById(u.getBusinessId()).orElse(null);
        return new MeResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getBusinessId(), u.getStaffId(),
                b == null ? null : b.getName(), b == null ? null : b.getSlug());
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
