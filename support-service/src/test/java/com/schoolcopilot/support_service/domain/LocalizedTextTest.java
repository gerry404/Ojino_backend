package com.schoolcopilot.support_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalizedTextTest {

    @Test
    void rendLAnglaisQuandIlEstPresent() {
        LocalizedText texte = new LocalizedText("Bonjour", "Hello");

        assertThat(texte.forLanguage("en")).isEqualTo("Hello");
    }

    @Test
    void replieSurLeFrancaisQuandLaTraductionManque() {
        assertThat(new LocalizedText("Bonjour", null).forLanguage("en"))
                .isEqualTo("Bonjour");
        assertThat(new LocalizedText("Bonjour", "").forLanguage("en"))
                .isEqualTo("Bonjour");
        assertThat(new LocalizedText("Bonjour", "   ").forLanguage("en"))
                .isEqualTo("Bonjour");
    }

    /**
     * La constante est comparee a la variable, jamais l'inverse : une langue
     * absente doit donner du francais, pas une NullPointerException.
     */
    @Test
    void supporteUneLangueNulle() {
        assertThat(new LocalizedText("Bonjour", "Hello").forLanguage(null))
                .isEqualTo("Bonjour");
    }
}
