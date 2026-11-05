package vn.educenter.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.educenter.common.BusinessException;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
        return user;
    }

    /** Tenant của người dùng hiện tại; SUPER_ADMIN không có tenant nên không gọi được API nghiệp vụ. */
    public static Long tenantId() {
        Long tenantId = get().tenantId();
        if (tenantId == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản không thuộc trung tâm nào");
        }
        return tenantId;
    }
}
