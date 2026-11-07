package vn.bookeasy.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.bookeasy.auth.AuthDtos.*;
import vn.bookeasy.common.ClientIp;
import vn.bookeasy.common.RateLimiter;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RateLimiter rateLimiter;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        rateLimiter.check("register:" + ClientIp.of(http), 5, 5);
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        rateLimiter.check("login:" + ClientIp.of(http), 10, 10);
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshRequest req) {
        authService.logout(req == null ? null : req.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me(CurrentUser.get().userId());
    }
}
