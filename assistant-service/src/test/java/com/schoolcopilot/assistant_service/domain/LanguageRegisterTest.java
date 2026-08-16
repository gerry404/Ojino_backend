package com.schoolcopilot.assistant_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'ecart de registre entre la maternelle et la prepa est la contrainte la plus
 * dure du produit.
 */
class LanguageRegisterTest {

    @Test
    @DisplayName("chaque cycle a son registre")
    void everyCycleMapsToItsRegister() {
        assertThat(LanguageRegister.forCycle("EARLY_YEARS"))
                .isEqualTo(LanguageRegister.EARLY_YEARS);
        assertThat(LanguageRegister.forCycle("COLLEGE")).isEqualTo(LanguageRegister.COLLEGE);
        assertThat(LanguageRegister.forCycle("HIGH_SCHOOL"))
                .isEqualTo(LanguageRegister.HIGH_SCHOOL);
        assertThat(LanguageRegister.forCycle("PREPA")).isEqualTo(LanguageRegister.PREPA);
        assertThat(LanguageRegister.forCycle("UNIVERSITY"))
                .isEqualTo(LanguageRegister.UNIVERSITY);
    }

    @Test
    @DisplayName("un cycle inconnu retombe sur le registre le moins risque")
    void unknownCycleFallsBackToTheSafestRegister() {
        // Trop simple pour un lyceen reste comprehensible ; trop complexe pour un
        // enfant de six ans ne l'est pas.
        assertThat(LanguageRegister.forCycle("INCONNU")).isEqualTo(LanguageRegister.COLLEGE);
        assertThat(LanguageRegister.forCycle(null)).isEqualTo(LanguageRegister.COLLEGE);
    }

    @Test
    @DisplayName("la longueur autorisee croit avec le cycle")
    void replyLengthGrowsWithTheCycle() {
        // Une reponse de deux mille caracteres a un enfant de grande section est
        // un echec meme si chaque mot est juste : il ne la lira pas.
        assertThat(LanguageRegister.EARLY_YEARS.maxReplyChars())
                .isLessThan(LanguageRegister.COLLEGE.maxReplyChars());
        assertThat(LanguageRegister.COLLEGE.maxReplyChars())
                .isLessThan(LanguageRegister.HIGH_SCHOOL.maxReplyChars());
        assertThat(LanguageRegister.HIGH_SCHOOL.maxReplyChars())
                .isLessThan(LanguageRegister.PREPA.maxReplyChars());
    }

    @Test
    @DisplayName("chaque registre porte une consigne exploitable")
    void everyRegisterHasGuidance() {
        for (LanguageRegister register : LanguageRegister.values()) {
            assertThat(register.guidance())
                    .as("consigne de %s", register)
                    .isNotBlank()
                    .hasSizeGreaterThan(50);
            assertThat(register.maxSentencesPerIdea())
                    .as("phrases par idee pour %s", register)
                    .isPositive();
        }
    }

    @Test
    @DisplayName("la maternelle interdit explicitement le jargon")
    void earlyYearsForbidsJargon() {
        assertThat(LanguageRegister.EARLY_YEARS.guidance())
                .containsIgnoringCase("aucun terme technique");
    }
}
