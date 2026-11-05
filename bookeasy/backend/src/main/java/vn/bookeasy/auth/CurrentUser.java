package vn.bookeasy.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.bookeasy.common.ApiException;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) return p;
        throw ApiException.unauthorized("Vui lòng đăng nhập");
    }

    /** businessId của người dùng hiện tại – nguồn duy nhất xác định tenant. */
    public static Long businessId() {
        Long bid = get().businessId();
        if (bid == null) throw ApiException.forbidden("Tài khoản không thuộc cơ sở nào");
        return bid;
    }
}
