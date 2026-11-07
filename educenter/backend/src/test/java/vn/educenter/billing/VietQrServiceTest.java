package vn.educenter.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import vn.educenter.common.BusinessException;
import vn.educenter.tenant.Tenant;

class VietQrServiceTest {

    @Test
    void buildsImgVietQrUrlWithAsciiNote() {
        Tenant t = new Tenant();
        t.setBankCode("VCB");
        t.setBankAccountNo("1023456789");
        t.setBankAccountName("Trung tâm Hoa Sen");
        var qr = new VietQrService().build(t, new BigDecimal("1250000"), "HD2611-0001", "Nguyễn Gia Hân");
        assertThat(qr.imageUrl()).startsWith("https://img.vietqr.io/image/VCB-1023456789-compact2.png?amount=1250000&addInfo=");
        assertThat(qr.note()).isEqualTo("HD2611-0001 NGUYEN GIA HAN");
        assertThat(qr.imageUrl()).contains("addInfo=HD2611-0001%20NGUYEN%20GIA%20HAN").contains("accountName=TRUNG%20TAM%20HOA%20SEN");
        assertThat(qr.bankName()).isEqualTo("Vietcombank");
    }

    @Test
    void requiresBankInfo() {
        assertThatThrownBy(() -> new VietQrService().build(new Tenant(), BigDecimal.TEN, "HD", "A"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ngân hàng");
    }
}
