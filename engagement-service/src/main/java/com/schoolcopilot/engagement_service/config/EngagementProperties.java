package com.schoolcopilot.engagement_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglages de la motivation.
 *
 * <p>Externalises parce que ce sont des choix pedagogiques, pas des constantes
 * techniques. Le bon nombre de jokers n'est pas le meme pour un enfant de six ans
 * et pour un etudiant en prepa, et cela s'ajustera en observant de vrais usages.
 *
 * @param initialFreezes jokers accordes a la creation du compte
 * @param maxFreezes plafond de la reserve. Sans plafond, quelqu'un qui s'absente
 *        trois mois reviendrait avec une serie intacte, ce qui viderait la
 *        mecanique de son sens.
 * @param freezesPerWeek rechargement hebdomadaire
 * @param streakAtRiskHour heure locale a partir de laquelle on previent que la
 *        serie va se rompre
 * @param moodWindowDays fenetre d'observation de l'humeur pour detecter une
 *        surcharge
 * @param lowMoodThreshold en dessous de cette moyenne, l'humeur est jugee basse
 * @param highWorkloadThreshold au-dessus, la charge est jugee lourde
 */
@ConfigurationProperties(prefix = "ojino.engagement")
public record EngagementProperties(
        int initialFreezes,
        int maxFreezes,
        int freezesPerWeek,
        int streakAtRiskHour,
        int moodWindowDays,
        double lowMoodThreshold,
        double highWorkloadThreshold) {
}
