package vn.starter.zma.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;

public final class CurrentUser {
    private CurrentUser() {}

    public static AuthUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthUser u) return u;
        throw new AppException(ErrorCode.UNAUTHORIZED);
    }
}
