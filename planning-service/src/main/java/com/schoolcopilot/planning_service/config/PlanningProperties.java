package com.schoolcopilot.planning_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Les reglages du planificateur.
 *
 * <p>Externalises parce que ce sont des choix pedagogiques, pas des constantes
 * techniques : la bonne duree d'une seance n'est pas la meme en CP et en prepa,
 * et cela s'ajustera en observant de vrais eleves.
 *
 * @param sessionMinutes duree visee d'une seance
 * @param minSessionMinutes en dessous, un reste de creneau est laisse libre
 *        plutot que de produire une seance trop courte pour etre utile
 * @param breakMinutes pause entre deux seances d'un meme creneau
 * @param maxSessionsPerDay plafond quotidien, pour ne pas produire un planning
 *        intenable qui se fera abandonner des la premiere semaine
 * @param spacedReviewDays delai avant de reproposer une notion acquise
 */
@ConfigurationProperties(prefix = "ojino.planning")
public record PlanningProperties(
        int sessionMinutes,
        int minSessionMinutes,
        int breakMinutes,
        int maxSessionsPerDay,
        int spacedReviewDays) {
}
