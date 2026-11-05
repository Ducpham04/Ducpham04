package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Casso (https://developer.casso.vn): webhook {"error":0,"data":[{"id":..,"tid":"..","description":"..","amount":..}]}
 * (v2 gửi "data" là object). Header "Secure-Token" chứa khóa bảo mật cấu hình trên Casso.
 */
@Component
public class CassoParser implements PaymentWebhookParser {
    @Override
    public String provider() {
        return "casso";
    }

    @Override
    public List<IncomingTransfer> parse(JsonNode p) {
        JsonNode data = p.path("data");
        List<IncomingTransfer> list = new ArrayList<>();
        if (data.isArray()) data.forEach(n -> add(list, n));
        else if (data.isObject()) add(list, data);
        return list;
    }

    private void add(List<IncomingTransfer> list, JsonNode n) {
        long amount = n.path("amount").asLong();
        if (amount <= 0) return; // bỏ giao dịch chi
        String id = n.hasNonNull("tid") ? n.path("tid").asText() : n.path("id").asText();
        list.add(new IncomingTransfer(id, amount, n.path("description").asText("")));
    }
}
