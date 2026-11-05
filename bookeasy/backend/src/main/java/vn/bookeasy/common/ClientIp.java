package vn.bookeasy.common;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {
    private ClientIp() {
    }

    public static String of(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = req.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real : req.getRemoteAddr();
    }
}
