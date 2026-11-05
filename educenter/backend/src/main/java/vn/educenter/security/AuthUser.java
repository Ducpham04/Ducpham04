package vn.educenter.security;

import java.security.Principal;

/** Người dùng đã xác thực, giải mã từ access token. */
public record AuthUser(Long userId, Long tenantId, Role role, Long studentId, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public boolean isManager() {
        return role == Role.CENTER_ADMIN || role == Role.STAFF;
    }

    public boolean isTeacher() {
        return role == Role.TEACHER;
    }
}
