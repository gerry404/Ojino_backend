package com.schoolcopilot.notification_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les heures de silence protegent des enfants : elles doivent etre justes, y
 * compris dans le cas qui casse les comparaisons naives — la plage qui traverse
 * minuit.
 */
class QuietHoursTest {

    /** Vingt et une heures a sept heures : le reglage par defaut. */
    private final QuietHours overnight = QuietHours.defaults();

    @Test
    @DisplayName("vingt-trois heures tombe dans une plage qui traverse minuit")
    void lateEveningIsCovered() {
        assertThat(overnight.covers(LocalTime.of(23, 0))).isTrue();
    }

    @Test
    @DisplayName("trois heures du matin aussi")
    void earlyMorningIsCovered() {
        // C'est ici qu'une comparaison "heure > debut && heure < fin" echoue.
        assertThat(overnight.covers(LocalTime.of(3, 0))).isTrue();
    }

    @Test
    @DisplayName("l'apres-midi n'est pas couvert")
    void afternoonIsNotCovered() {
        assertThat(overnight.covers(LocalTime.of(15, 0))).isFalse();
    }

    @Test
    @DisplayName("le debut est inclus, la fin exclue")
    void boundariesAreHalfOpen() {
        assertThat(overnight.covers(LocalTime.of(21, 0))).isTrue();
        assertThat(overnight.covers(LocalTime.of(7, 0))).isFalse();
    }

    @Test
    @DisplayName("une plage dans la journee fonctionne aussi")
    void daytimeRangeWorks() {
        QuietHours schoolHours = new QuietHours(LocalTime.of(8, 0), LocalTime.of(16, 0), true);

        assertThat(schoolHours.covers(LocalTime.of(10, 0))).isTrue();
        assertThat(schoolHours.covers(LocalTime.of(20, 0))).isFalse();
    }

    @Test
    @DisplayName("desactivees, elles ne couvrent rien")
    void disabledCoversNothing() {
        QuietHours disabled = new QuietHours(LocalTime.of(21, 0), LocalTime.of(7, 0), false);

        assertThat(disabled.covers(LocalTime.of(3, 0))).isFalse();
    }

    @Test
    @DisplayName("un envoi du soir est reporte au lendemain matin")
    void eveningIsDeferredToNextMorning() {
        LocalDateTime tuesdayEvening = LocalDateTime.of(2026, 8, 18, 23, 30);

        assertThat(overnight.nextAllowed(tuesdayEvening))
                .isEqualTo(LocalDateTime.of(2026, 8, 19, 7, 0));
    }

    @Test
    @DisplayName("un envoi de la nuit est reporte au matin meme")
    void nightIsDeferredToTheSameMorning() {
        LocalDateTime wednesdayNight = LocalDateTime.of(2026, 8, 19, 3, 0);

        // Trois heures du matin : la fin de plage est le matin qui suit
        // immediatement, pas celui du lendemain.
        assertThat(overnight.nextAllowed(wednesdayNight))
                .isEqualTo(LocalDateTime.of(2026, 8, 19, 7, 0));
    }

    @Test
    @DisplayName("hors plage, l'envoi n'est pas repousse")
    void outsideQuietHoursNothingMoves() {
        LocalDateTime afternoon = LocalDateTime.of(2026, 8, 19, 15, 0);

        assertThat(overnight.nextAllowed(afternoon)).isEqualTo(afternoon);
    }
}
