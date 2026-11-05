package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cổng giả lập dùng cho dev/test: {"txnId":"T1","amount":150000,"content":"BEAB12CD"}. */
@Component
public class MockParser implements PaymentWebhookParser {
    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public List<IncomingTransfer> parse(JsonNode p) {
        return List.of(new IncomingTransfer(p.path("txnId").asText(), p.path("amount").asLong(), p.path("content").asText("")));
    }
}
