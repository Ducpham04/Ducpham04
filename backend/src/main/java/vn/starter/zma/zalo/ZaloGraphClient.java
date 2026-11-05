package vn.starter.zma.zalo;

/**
 * Cổng giao tiếp với Zalo Graph API.
 * <ul>
 *   <li>{@link HttpZaloGraphClient}: gọi graph.zalo.me thật.</li>
 *   <li>{@link MockZaloGraphClient}: dùng khi app.zalo.mock=true (demo / dev / test).</li>
 * </ul>
 */
public interface ZaloGraphClient {

    /** Xác thực access token của người dùng (lấy từ zmp-sdk getAccessToken) và trả về hồ sơ. */
    ZaloProfile getProfile(String userAccessToken);

    /** Giải mã token số điện thoại (zmp-sdk getPhoneNumber) → số dạng 84xxxxxxxxx. */
    String decodePhoneNumber(String userAccessToken, String phoneToken);

    /** Giải mã token vị trí (zmp-sdk getLocation). */
    ZaloLocation decodeLocation(String userAccessToken, String locationToken);
}
