package vn.educenter.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VnTextTest {

    @Test
    void stripsVietnameseAccents() {
        assertThat(VnText.stripAccents("Đặng Quốc Huy – lớp Ưu tú")).isEqualTo("Dang Quoc Huy – lop Uu tu");
    }

    @Test
    void transferNoteIsAsciiUppercaseMax50() {
        assertThat(VnText.transferNote("HD2611-0001", "Lê Hoàng Bảo Anh")).isEqualTo("HD2611-0001 LE HOANG BAO ANH");
        assertThat(VnText.transferNote("x".repeat(80))).hasSize(50);
    }

    @Test
    void readsAmountsInVietnamese() {
        assertThat(VnText.amountInWords(0)).isEqualTo("Không đồng");
        assertThat(VnText.amountInWords(1_250_000)).isEqualTo("Một triệu hai trăm năm mươi nghìn đồng");
        assertThat(VnText.amountInWords(7_200_000)).isEqualTo("Bảy triệu hai trăm nghìn đồng");
        assertThat(VnText.amountInWords(1_005)).isEqualTo("Một nghìn không trăm lẻ năm đồng");
        assertThat(VnText.amountInWords(21_015)).isEqualTo("Hai mươi mốt nghìn không trăm mười lăm đồng");
        assertThat(VnText.amountInWords(45_000)).isEqualTo("Bốn mươi lăm nghìn đồng");
    }

    @Test
    void formatsVnd() {
        assertThat(VnText.formatVnd(new BigDecimal("1250000"))).isEqualTo("1.250.000 ₫");
    }
}
