package vn.bookeasy.payment;

import org.springframework.stereotype.Service;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingDtos.PaymentInfo;
import vn.bookeasy.booking.PaymentStatus;
import vn.bookeasy.business.Business;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Dựng URL ảnh QR theo chuẩn img.vietqr.io (NAPAS 247). */
@Service
public class VietQrService {
    public String qrUrl(String bankBin, String accountNo, String accountName, long amount, String content) {
        StringBuilder sb = new StringBuilder("https://img.vietqr.io/image/")
                .append(enc(bankBin)).append('-').append(enc(accountNo)).append("-compact2.png")
                .append("?amount=").append(amount)
                .append("&addInfo=").append(enc(content));
        if (accountName != null) sb.append("&accountName=").append(enc(accountName));
        return sb.toString();
    }

    public PaymentInfo paymentInfo(Business b, Booking booking) {
        if (booking.getPaymentStatus() != PaymentStatus.PENDING || !b.paymentConfigured()) return null;
        long remaining = Math.max(0, booking.getAmountDue() - booking.getAmountPaid());
        return new PaymentInfo(qrUrl(b.getBankBin(), b.getBankAccountNo(), b.getBankAccountName(), remaining, booking.getCode()),
                b.getBankBin(), b.getBankAccountNo(), b.getBankAccountName(), remaining, booking.getCode());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
