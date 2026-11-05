package vn.educenter.common;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

/** Tiện ích chữ tiếng Việt: bỏ dấu, định dạng tiền, đọc số thành chữ. */
public final class VnText {

    private static final String[] DIGITS = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    private static final String[] UNITS = {"", " nghìn", " triệu", " tỷ", " nghìn tỷ", " triệu tỷ"};

    private VnText() {
    }

    public static String stripAccents(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.replace('đ', 'd').replace('Đ', 'D');
    }

    /** Nội dung chuyển khoản: không dấu, in hoa, chỉ chữ số + khoảng trắng + gạch ngang, tối đa 50 ký tự. */
    public static String transferNote(String... parts) {
        String joined = String.join(" ", parts);
        String ascii = stripAccents(joined).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 \\-]", "").replaceAll("\\s+", " ").trim();
        return ascii.length() > 50 ? ascii.substring(0, 50).trim() : ascii;
    }

    public static String formatVnd(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount == null ? BigDecimal.ZERO : amount) + " ₫";
    }

    public static String amountInWords(long number) {
        if (number == 0) return "Không đồng";
        if (number < 0) return "Âm " + amountInWords(-number).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        int unitIndex = 0;
        String result = "";
        long n = number;
        while (n > 0) {
            int group = (int) (n % 1000);
            n /= 1000;
            if (group > 0) {
                String words = readGroup(group, n > 0);
                result = words + UNITS[unitIndex] + (result.isEmpty() ? "" : " " + result);
            }
            unitIndex++;
        }
        sb.append(result.trim()).append(" đồng");
        String s = sb.toString().replaceAll("\\s+", " ");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String readGroup(int group, boolean hasHigher) {
        int hundreds = group / 100;
        int tens = (group % 100) / 10;
        int ones = group % 10;
        StringBuilder sb = new StringBuilder();
        if (hundreds > 0 || hasHigher) {
            sb.append(DIGITS[hundreds]).append(" trăm");
        }
        if (tens > 1) {
            sb.append(' ').append(DIGITS[tens]).append(" mươi");
            if (ones == 1) sb.append(" mốt");
            else if (ones == 4) sb.append(" tư");
            else if (ones == 5) sb.append(" lăm");
            else if (ones > 0) sb.append(' ').append(DIGITS[ones]);
        } else if (tens == 1) {
            sb.append(" mười");
            if (ones == 5) sb.append(" lăm");
            else if (ones > 0) sb.append(' ').append(DIGITS[ones]);
        } else if (ones > 0) {
            if (hundreds > 0 || hasHigher) sb.append(" lẻ");
            sb.append(' ').append(DIGITS[ones]);
        }
        return sb.toString().trim();
    }
}
