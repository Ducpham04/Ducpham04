package vn.educenter.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import vn.educenter.common.BusinessException;
import vn.educenter.common.VnText;
import vn.educenter.tenant.Tenant;

/**
 * Sinh link ảnh QR chuyển khoản qua dịch vụ công khai img.vietqr.io (chuẩn VietQR/NAPAS, quét bằng mọi app ngân hàng).
 * Port từ src/lib/vietqr.ts của bản cũ.
 */
@Service
public class VietQrService {

    public static final Map<String, String> BANKS = new LinkedHashMap<>();

    static {
        BANKS.put("VCB", "Vietcombank");
        BANKS.put("BIDV", "BIDV");
        BANKS.put("ICB", "VietinBank");
        BANKS.put("VBA", "Agribank");
        BANKS.put("TCB", "Techcombank");
        BANKS.put("MB", "MB Bank");
        BANKS.put("ACB", "ACB");
        BANKS.put("VPB", "VPBank");
        BANKS.put("TPB", "TPBank");
        BANKS.put("STB", "Sacombank");
        BANKS.put("HDB", "HDBank");
        BANKS.put("VIB", "VIB");
        BANKS.put("SHB", "SHB");
        BANKS.put("EIB", "Eximbank");
        BANKS.put("MSB", "MSB");
        BANKS.put("OCB", "OCB");
        BANKS.put("SEAB", "SeABank");
        BANKS.put("LPB", "LPBank");
        BANKS.put("NAB", "Nam A Bank");
        BANKS.put("BAB", "Bac A Bank");
    }

    public BillingDtos.VietQrResponse build(Tenant tenant, BigDecimal amount, String invoiceCode, String studentName) {
        if (!tenant.hasBankInfo()) {
            throw new BusinessException("Trung tâm chưa khai báo tài khoản ngân hàng. Vào Cài đặt → Ngân hàng để thêm.");
        }
        String note = VnText.transferNote(invoiceCode, studentName);
        String url = buildImageUrl(tenant.getBankCode(), tenant.getBankAccountNo(), tenant.getBankAccountName(), amount, note);
        return new BillingDtos.VietQrResponse(url, tenant.getBankCode(), BANKS.getOrDefault(tenant.getBankCode(), tenant.getBankCode()),
                tenant.getBankAccountNo(), tenant.getBankAccountName(), amount, note);
    }

    public static String buildImageUrl(String bankCode, String accountNo, String accountName, BigDecimal amount, String note) {
        long rounded = amount == null ? 0 : Math.max(0, amount.setScale(0, RoundingMode.HALF_UP).longValue());
        StringBuilder sb = new StringBuilder("https://img.vietqr.io/image/")
                .append(enc(bankCode)).append('-').append(enc(accountNo)).append("-compact2.png")
                .append("?amount=").append(rounded)
                .append("&addInfo=").append(enc(note));
        if (accountName != null && !accountName.isBlank()) {
            sb.append("&accountName=").append(enc(VnText.stripAccents(accountName).toUpperCase()));
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
