package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Chuyển payload webhook của từng cổng thanh toán thành {@link IncomingTransfer}.
 * Thêm cổng mới = thêm một bean implement interface này.
 */
public interface PaymentWebhookParser {
    String provider();

    List<IncomingTransfer> parse(JsonNode payload);
}
