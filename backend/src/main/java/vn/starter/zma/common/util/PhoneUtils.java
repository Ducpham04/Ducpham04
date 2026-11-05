package vn.starter.zma.common.util;

public final class PhoneUtils {
    private PhoneUtils() {}

    /** Chuẩn hoá 84xxxxxxxxx / +84xxxxxxxxx → 0xxxxxxxxx. */
    public static String normalizeVn(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    /** Che số điện thoại khi ghi log: 0912345678 → 091****678. */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    /** Định dạng gửi ZNS: 0xxxxxxxxx → 84xxxxxxxxx. */
    public static String toInternational(String phone) {
        if (phone == null) return null;
        return phone.startsWith("0") ? "84" + phone.substring(1) : phone;
    }
}
