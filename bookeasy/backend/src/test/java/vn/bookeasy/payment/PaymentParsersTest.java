package vn.bookeasy.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentParsersTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test
    void parsesSepayIncomingTransfer() throws Exception {
        var t = new SepayParser().parse(om.readTree("""
                {"id":92704,"gateway":"Vietcombank","transactionDate":"2030-01-07 14:02:37","accountNumber":"0123499999",
                 "code":null,"content":"MBVCB.3278 BEAB12CD thanh toan","transferType":"in","transferAmount":84000,
                 "accumulated":19077000,"referenceCode":"MBVCB.3278907687","description":""}"""));
        assertThat(t).singleElement().satisfies(x -> {
            assertThat(x.txnId()).isEqualTo("92704");
            assertThat(x.amount()).isEqualTo(84000);
            assertThat(x.content()).contains("BEAB12CD");
        });
    }

    @Test
    void ignoresSepayOutgoingTransfer() throws Exception {
        assertThat(new SepayParser().parse(om.readTree("{\"id\":1,\"transferType\":\"out\",\"transferAmount\":5}"))).isEmpty();
    }

    @Test
    void parsesCassoArrayAndObjectPayloads() throws Exception {
        var v1 = new CassoParser().parse(om.readTree("""
                {"error":0,"data":[{"id":1,"tid":"TF1","description":"CK BEXY34ZZ","amount":150000},
                                   {"id":2,"tid":"TF2","description":"rut tien","amount":-50000}]}"""));
        assertThat(v1).singleElement().satisfies(x -> assertThat(x.txnId()).isEqualTo("TF1"));
        var v2 = new CassoParser().parse(om.readTree("{\"error\":0,\"data\":{\"id\":77,\"description\":\"BEXY34ZZ\",\"amount\":10}}"));
        assertThat(v2).singleElement().satisfies(x -> assertThat(x.txnId()).isEqualTo("77"));
    }

    @Test
    void extractsBookingCodeFromNoisyContent() {
        assertThat(PaymentService.extractCode("IBFT beab12cd-Nguyen Van A chuyen tien")).contains("BEAB12CD");
        assertThat(PaymentService.extractCode("MBVCB.123.BEAB12CD.CT")).contains("BEAB12CD");
        assertThat(PaymentService.extractCode("chuyen khoan tien phong")).isEmpty();
        assertThat(PaymentService.extractCode(null)).isEmpty();
    }

    @Test
    void buildsVietQrUrl() {
        String url = new VietQrService().qrUrl("970436", "1023456789", "CONG TY ABC", 84000, "BEAB12CD");
        assertThat(url).isEqualTo("https://img.vietqr.io/image/970436-1023456789-compact2.png?amount=84000&addInfo=BEAB12CD&accountName=CONG%20TY%20ABC");
    }
}
