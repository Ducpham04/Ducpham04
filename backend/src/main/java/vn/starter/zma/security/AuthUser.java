package vn.starter.zma.security;

import vn.starter.zma.user.Role;

/** Principal gắn vào SecurityContext sau khi xác thực JWT. */
public record AuthUser(Long id, Role role) {
    public boolean isAdmin() { return role == Role.ADMIN; }
}
