package com.schoolcopilot.notification_service.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * La plage ou l'on ne derange pas.
 *
 * <p>Ce n'est pas du confort. L'application s'adresse a des enfants des la
 * maternelle : leur envoyer une notification a vingt-trois heures est un probleme
 * en soi, et un point de conformite dans plusieurs pays.
 *
 * <p>La plage traverse minuit dans le cas courant — 21h a 7h. C'est le detail qui
 * fait qu'une comparaison naive {@code heure > debut && heure < fin} ne marche pas.
 */
public record QuietHours(LocalTime from, LocalTime to, boolean enabled) {

    /** Vingt et une heures a sept heures : un enfant dort, un lyceen revise. */
    public static QuietHours defaults() {
        return new QuietHours(LocalTime.of(21, 0), LocalTime.of(7, 0), true);
    }

    public boolean covers(LocalTime moment) {
        if (!enabled) {
            return false;
        }
        if (from.equals(to)) {
            // Plage vide : personne n'est jamais derange, ce qui est un reglage
            // legitime meme s'il est inhabituel.
            return true;
        }
        return from.isBefore(to)
                ? !moment.isBefore(from) && moment.isBefore(to)
                // La plage passe minuit : on est dedans si l'on est apres le debut
                // OU avant la fin.
                : !moment.isBefore(from) || moment.isBefore(to);
    }

    /**
     * Le premier instant ou l'envoi redevient possible.
     *
     * <p>Une notification tombee dans la plage est <strong>reportee</strong>, jamais
     * jetee : un rappel de seance perdu, c'est une seance manquee.
     */
    public LocalDateTime nextAllowed(LocalDateTime moment) {
        if (!covers(moment.toLocalTime())) {
            return moment;
        }
        LocalDateTime candidate = moment.toLocalDate().atTime(to);
        // Si la fin de plage est deja passee dans la journee, elle tombe demain.
        return candidate.isAfter(moment) ? candidate : candidate.plusDays(1);
    }
}
