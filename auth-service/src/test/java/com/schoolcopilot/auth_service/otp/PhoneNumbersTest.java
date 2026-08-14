package com.schoolcopilot.auth_service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.schoolcopilot.auth_service.exception.AuthException;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "+237690000000",
            "+237 690 00 00 00",
            "+237-690-000-000",
            "+237 (690) 000.000"
    })
    @DisplayName("les separateurs habituels sont acceptes et effaces")
    void normalizesCommonFormats(String input) {
        assertThat(PhoneNumbers.normalize(input)).isEqualTo("+237690000000");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "690000000",      // sans indicatif
            "00237690000000", // prefixe 00 au lieu de +
            "+0123456789",    // indicatif commencant par zero
            "+123",           // trop court
            "+237690000000000000", // trop long
            "abc"
    })
    @DisplayName("tout ce qui n'est pas du E.164 est refuse")
    void rejectsInvalidFormats(String input) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(input))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_phone");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    @DisplayName("un numero vide est refuse")
    void rejectsBlank(String input) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(input))
                .isInstanceOf(AuthException.class);
    }
}
