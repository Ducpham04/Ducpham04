package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SePay (https://docs.sepay.vn): webhook POST JSON, header "Authorization: Apikey {API_KEY}".
 * Ví dụ: {"id":92704,"gateway":"Vietcombank","content":"BEAB12CD thanh toan","transferType":"in","transferAmount":150000,...}
 */
@Component
public class SepayParser implements PaymentWebhookParser {
    @Override
    public String provider() {
        return "sepay";
    }

    @Override
    public List<IncomingTransfer> parse(JsonNode p) {
        if (!"in".equalsIgnoreCase(p.path("transferType").asText("in"))) return List.of();
        String content = p.path("content").asText("") + " " + p.path("description").asText("") + " " + p.path("code").asText("");
        return List.of(new IncomingTransfer(p.path("id").asText(), p.path("transferAmount").asLong(), content.trim()));
    }
}
