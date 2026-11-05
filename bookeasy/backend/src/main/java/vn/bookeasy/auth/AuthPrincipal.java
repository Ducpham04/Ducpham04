package vn.bookeasy.auth;

public record AuthPrincipal(Long userId, String email, Role role, Long businessId, Long staffId) {
    public boolean isOwner() {
        return role == Role.OWNER;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }
}
