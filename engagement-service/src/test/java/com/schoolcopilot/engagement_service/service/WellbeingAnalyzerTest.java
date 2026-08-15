package com.schoolcopilot.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.engagement_service.config.EngagementProperties;
import com.schoolcopilot.engagement_service.domain.MoodCheckIn;
import com.schoolcopilot.engagement_service.service.WellbeingAnalyzer.Signal;

/**
 * Le reperage des signes de surcharge.
 *
 * <p>Ces tests fixent surtout la prudence attendue : ne pas conclure trop vite, et
 * ne pas alarmer sur une periode d'examens normale.
 */
class WellbeingAnalyzerTest {

    private final WellbeingAnalyzer analyzer = new WellbeingAnalyzer(
            new EngagementProperties(2, 3, 1, 19, 7, 2.5, 4.0));

    private final LocalDate today = LocalDate.of(2026, 8, 19);

    @Test
    @DisplayName("sans releve, aucun signal")
    void noCheckInsNoSignal() {
        assertThat(analyzer.analyze(List.of(), today).signal()).isEqualTo(Signal.NONE);
    }

    @Test
    @DisplayName("deux releves ne suffisent pas a conclure")
    void twoCheckInsAreNotATrend() {
        List<MoodCheckIn> few = List.of(checkIn(1, 1, 5), checkIn(1, 2, 5));

        // Une mauvaise journee n'est pas une tendance.
        assertThat(analyzer.analyze(few, today).signal()).isEqualTo(Signal.NONE);
    }

    @Test
    @DisplayName("humeur basse et charge lourde signalent une surcharge")
    void lowMoodAndHeavyLoadMeansOverloaded() {
        List<MoodCheckIn> window = window(2, 5);

        assertThat(analyzer.analyze(window, today).signal()).isEqualTo(Signal.OVERLOADED);
    }

    @Test
    @DisplayName("humeur basse sans charge particuliere est un signal distinct")
    void lowMoodAloneIsItsOwnSignal() {
        // On peut aller mal sans etre deborde : confondre les deux ferait passer a
        // cote de la moitie des situations.
        assertThat(analyzer.analyze(window(2, 2), today).signal()).isEqualTo(Signal.LOW_MOOD);
    }

    @Test
    @DisplayName("une charge lourde avec le moral tenu n'alarme pas")
    void heavyLoadWithGoodMoodDoesNotAlarm() {
        WellbeingAnalyzer.Assessment assessment = analyzer.analyze(window(4, 5), today);

        // C'est une periode d'examens normale : alerter reviendrait a crier au loup.
        assertThat(assessment.signal()).isEqualTo(Signal.HEAVY_LOAD);
        assertThat(assessment.warrantsAttention()).isFalse();
    }

    @Test
    @DisplayName("tout va bien : aucun signal")
    void healthyIsSilent() {
        assertThat(analyzer.analyze(window(4, 2), today).signal()).isEqualTo(Signal.NONE);
    }

    @Test
    @DisplayName("une degradation ressort dans la tendance meme si la moyenne rassure")
    void decliningTrendIsVisible() {
        // De 5 a 1 : la moyenne vaut 3, la meme que quelqu'un de stable.
        List<MoodCheckIn> declining = List.of(
                checkIn(5, 6, 3), checkIn(5, 5, 3), checkIn(3, 4, 3),
                checkIn(1, 2, 3), checkIn(1, 1, 3));

        assertThat(analyzer.analyze(declining, today).trend()).isNegative();
    }

    @Test
    @DisplayName("quelqu'un qui repondait puis s'est tu est signale")
    void goneQuietIsFlagged() {
        List<MoodCheckIn> old = List.of(checkIn(4, 30, 3), checkIn(4, 31, 3));

        assertThat(analyzer.analyze(old, today).signal()).isEqualTo(Signal.DISENGAGED);
    }

    @Test
    @DisplayName("les releves hors fenetre ne comptent pas")
    void oldCheckInsAreOutOfWindow() {
        List<MoodCheckIn> mixed = new ArrayList<>(window(4, 2));
        mixed.add(checkIn(1, 30, 5));

        // Une periode difficile il y a un mois ne doit pas peser sur le present.
        assertThat(analyzer.analyze(mixed, today).signal()).isEqualTo(Signal.NONE);
    }

    /** Cinq releves dans la fenetre, tous a la meme humeur et la meme charge. */
    private List<MoodCheckIn> window(int mood, int workload) {
        List<MoodCheckIn> items = new ArrayList<>();
        for (int daysAgo = 0; daysAgo < 5; daysAgo++) {
            items.add(checkIn(mood, daysAgo, workload));
        }
        return items;
    }

    private MoodCheckIn checkIn(int mood, int daysAgo, int workload) {
        LocalDate day = today.minusDays(daysAgo);
        return new MoodCheckIn(MoodCheckIn.idFor("u", day), "u", day, mood, workload, null,
                Instant.now());
    }
}
