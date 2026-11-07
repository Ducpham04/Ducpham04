package vn.bookeasy.booking;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class BookingCodeGenerator {
    /** Bỏ ký tự dễ nhầm (0/O, 1/I) để khách đọc/gõ nội dung chuyển khoản chính xác. */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder("BE");
        for (int i = 0; i < 6; i++) sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        return sb.toString();
    }
}
