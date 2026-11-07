package vn.bookeasy.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextUtils {
    private static final Pattern VN_PHONE = Pattern.compile("^0(3|5|7|8|9)\\d{8}$");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private TextUtils() {
    }

    /** Chuẩn hóa SĐT Việt Nam về dạng 0xxxxxxxxx, trả null nếu không hợp lệ. */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String p = raw.replaceAll("[\\s.\\-()]", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        else if (p.startsWith("84") && p.length() == 11) p = "0" + p.substring(2);
        return VN_PHONE.matcher(p).matches() ? p : null;
    }

    public static String slugify(String input) {
        if (input == null) return "";
        String s = input.replace('đ', 'd').replace('Đ', 'D');
        s = DIACRITICS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.length() > 60 ? s.substring(0, 60).replaceAll("-$", "") : s;
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
