package vn.bookeasy.payment;

/** Giao dịch chuyển khoản đến đã chuẩn hóa từ mọi cổng (SePay, Casso, mock). */
public record IncomingTransfer(String txnId, long amount, String content) {
}
