package vn.starter.zma.zalo;

import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * Giả lập Zalo Graph API. Quy ước token:
 * <ul>
 *   <li>Access token: {@code mock-<zaloId>} hoặc {@code mock-<zaloId>|<Tên hiển thị>}</li>
 *   <li>Token SĐT: {@code mock-phone-<số>} (mặc định 84912345678)</li>
 *   <li>Token vị trí: {@code mock-location-<lat>,<lng>} (mặc định Hồ Gươm, Hà Nội)</li>
 * </ul>
 */
public class MockZaloGraphClient implements ZaloGraphClient {
    private static final String DEFAULT_NAME = "Nguyễn Văn An";

    @Override
    public ZaloProfile getProfile(String token) {
        if (token == null || !token.startsWith("mock-") || token.length() <= 5) {
            throw new AppException(ErrorCode.ZALO_TOKEN_INVALID);
        }
        String rest = token.substring(5);
        String id = rest;
        String name = DEFAULT_NAME;
        int sep = rest.indexOf('|');
        if (sep > 0) {
            id = rest.substring(0, sep);
            name = rest.substring(sep + 1);
        }
        if (!id.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new AppException(ErrorCode.ZALO_TOKEN_INVALID);
        }
        return new ZaloProfile(id, name, null);
    }

    @Override
    public String decodePhoneNumber(String userAccessToken, String phoneToken) {
        getProfile(userAccessToken);
        if (phoneToken == null || !phoneToken.startsWith("mock-phone")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Token số điện thoại không hợp lệ");
        }
        String n = phoneToken.length() > 11 ? phoneToken.substring(11) : "";
        return n.matches("\\d{9,12}") ? n : "84912345678";
    }

    @Override
    public ZaloLocation decodeLocation(String userAccessToken, String locationToken) {
        getProfile(userAccessToken);
        if (locationToken == null || !locationToken.startsWith("mock-location")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Token vị trí không hợp lệ");
        }
        try {
            String[] parts = locationToken.substring(14).split(",");
            return new ZaloLocation(new BigDecimal(parts[0]), new BigDecimal(parts[1]));
        } catch (RuntimeException e) {
            return new ZaloLocation(new BigDecimal("21.0287797"), new BigDecimal("105.8524247"));
        }
    }
}
