package com.schoolcopilot.engagement_service.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.engagement_service.config.EngagementProperties;
import com.schoolcopilot.engagement_service.domain.MoodCheckIn;

/**
 * Reperage des signes de decrochage ou de surcharge.
 *
 * <p>Logique pure et volontairement prudente. Ce composant ne diagnostique rien :
 * il signale des motifs qui meritent qu'on propose une pause, qu'on allege le
 * planning, ou qu'on invite a en parler a quelqu'un. Confondre un signal avec un
 * diagnostic serait une faute, et la nuance doit rester visible dans le code.
 *
 * <p>Il faut plusieurs releves pour conclure quoi que ce soit : une mauvaise
 * journee n'est pas une tendance.
 */
@Component
public class WellbeingAnalyzer {

    /** Nombre minimal de releves avant de se prononcer. */
    private static final int MIN_CHECK_INS = 3;

    private final EngagementProperties properties;

    public WellbeingAnalyzer(EngagementProperties properties) {
        this.properties = properties;
    }

    /** Ce qui ressort de la fenetre d'observation. */
    public enum Signal {

        /** Pas assez de releves, ou rien de notable. */
        NONE,

        /** Humeur basse et charge lourde : le motif de surcharge. */
        OVERLOADED,

        /** Humeur basse sans charge particuliere. */
        LOW_MOOD,

        /** Charge lourde mais moral tenu : a surveiller sans alarmer. */
        HEAVY_LOAD,

        /** Plus aucun releve depuis longtemps, alors qu'il y en avait. */
        DISENGAGED
    }

    /**
     * @param averageMood moyenne sur la fenetre, ou 0 si aucun releve
     * @param trend difference entre la seconde moitie et la premiere. Negatif
     *        signifie que ca se degrade.
     */
    public record Assessment(Signal signal, double averageMood, double averageWorkload,
            double trend, int checkInCount) {

        /**
         * Vrai si la situation merite qu'on propose quelque chose a l'eleve.
         *
         * <p>Une charge lourde seule ne declenche rien : c'est une periode
         * d'examens normale, et alerter dessus reviendrait a crier au loup.
         */
        public boolean warrantsAttention() {
            return signal == Signal.OVERLOADED || signal == Signal.LOW_MOOD
                    || signal == Signal.DISENGAGED;
        }
    }

    public Assessment analyze(List<MoodCheckIn> checkIns, LocalDate today) {
        List<MoodCheckIn> window = checkIns.stream()
                .filter(checkIn -> !checkIn.day()
                        .isBefore(today.minusDays(properties.moodWindowDays())))
                .sorted(Comparator.comparing(MoodCheckIn::day))
                .toList();

        if (window.size() < MIN_CHECK_INS) {
            return new Assessment(disengagementSignal(checkIns, today), 0, 0, 0, window.size());
        }

        double mood = average(window, MoodCheckIn::mood);
        double workload = average(window, MoodCheckIn::workload);
        double trend = trendOf(window);

        Signal signal = classify(mood, workload);
        return new Assessment(signal, round(mood), round(workload), round(trend), window.size());
    }

    // ------------------------------------------------------------------

    private Signal classify(double mood, double workload) {
        boolean lowMood = mood <= properties.lowMoodThreshold();
        boolean heavyLoad = workload >= properties.highWorkloadThreshold();

        if (lowMood && heavyLoad) {
            return Signal.OVERLOADED;
        }
        if (lowMood) {
            return Signal.LOW_MOOD;
        }
        if (heavyLoad) {
            return Signal.HEAVY_LOAD;
        }
        return Signal.NONE;
    }

    /**
     * Un eleve qui repondait puis s'est tu depuis deux fenetres est probablement
     * en train de decrocher. Quelqu'un qui n'a jamais repondu, en revanche, n'a
     * peut-etre simplement pas envie — et ce n'est pas un signal.
     */
    private Signal disengagementSignal(List<MoodCheckIn> checkIns, LocalDate today) {
        if (checkIns.isEmpty()) {
            return Signal.NONE;
        }

        LocalDate lastCheckIn = checkIns.stream()
                .map(MoodCheckIn::day)
                .max(LocalDate::compareTo)
                .orElseThrow();

        return lastCheckIn.isBefore(today.minusDays(2L * properties.moodWindowDays()))
                ? Signal.DISENGAGED
                : Signal.NONE;
    }

    /**
     * Compare la seconde moitie de la fenetre a la premiere.
     *
     * <p>Une moyenne seule masque une degradation : quelqu'un qui passe de 5 a 1 a
     * la meme moyenne que quelqu'un de stable a 3.
     */
    private double trendOf(List<MoodCheckIn> window) {
        int half = window.size() / 2;
        if (half == 0) {
            return 0;
        }
        double before = average(window.subList(0, half), MoodCheckIn::mood);
        double after = average(window.subList(window.size() - half, window.size()),
                MoodCheckIn::mood);
        return after - before;
    }

    private double average(List<MoodCheckIn> items,
            java.util.function.ToIntFunction<MoodCheckIn> field) {
        return items.stream().mapToInt(field).average().orElse(0);
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
