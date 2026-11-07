package vn.bookeasy.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {
    @ParameterizedTest
    @CsvSource({"0912345678,0912345678", "0912 345 678,0912345678", "+84912345678,0912345678",
            "84912345678,0912345678", "091.234.5678,0912345678", "0389-123-456,0389123456"})
    void normalizesValidPhones(String raw, String expected) {
        assertThat(TextUtils.normalizePhone(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "0212345678", "09123456789", "abcdefghij", "+1 555 1234"})
    void rejectsInvalidPhones(String raw) {
        assertThat(TextUtils.normalizePhone(raw)).isNull();
    }

    @ParameterizedTest
    @CsvSource({"Lotus Spa & Massage,lotus-spa-massage", "Nha khoa Đức Tâm,nha-khoa-duc-tam", "  Tóc Đẹp 24h  ,toc-dep-24h"})
    void slugifiesVietnamese(String input, String expected) {
        assertThat(TextUtils.slugify(input)).isEqualTo(expected);
    }
}
