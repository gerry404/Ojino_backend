package com.schoolcopilot.content_service;

import java.util.List;

import com.schoolcopilot.content_service.domain.reference.EducationLevel;
import com.schoolcopilot.content_service.domain.reference.EducationSystem;
import com.schoolcopilot.content_service.domain.reference.Subject;
import com.schoolcopilot.content_service.domain.reference.Track;

/** Un referentiel reduit, calque sur le systeme francophone camerounais. */
public final class TestFixtures {

    public static final String SYSTEM = "CM-FR";

    private TestFixtures() {
    }

    public static EducationSystem system() {
        return new EducationSystem(SYSTEM, "CM", "Cameroun", "Systeme francophone", "fr", 1, true);
    }

    public static List<EducationLevel> levels() {
        return List.of(
                level("6E", "6e", "COLLEGE", 1, 11, 12, false, false),
                level("5E", "5e", "COLLEGE", 2, 12, 13, false, false),
                level("4E", "4e", "COLLEGE", 3, 13, 14, false, false),
                level("3E", "3e", "COLLEGE", 4, 14, 15, false, false),
                level("2NDE", "Seconde", "LYCEE", 5, 15, 16, true, false),
                level("1ERE", "Premiere", "LYCEE", 6, 16, 17, true, false),
                level("TLE", "Terminale", "LYCEE", 7, 17, 19, true, false),
                // Ancienne classe retiree du systeme : elle ne doit plus etre
                // proposee, mais reste resolvable pour les profils qui la portent.
                level("1AC", "1re annee (ancien)", "COLLEGE", 0, 10, 11, false, true));
    }

    public static List<Track> tracks() {
        return List.of(
                track("SA", "Seconde A", "Litteraire", List.of("2NDE"), 1, false),
                track("C", "C", "Maths et sciences physiques", List.of("1ERE", "TLE"), 2, false),
                track("D", "D", "Maths et sciences de la vie", List.of("1ERE", "TLE"), 3, false),
                track("E", "E", "Filiere retiree", List.of("1ERE", "TLE"), 4, true));
    }

    public static List<Subject> subjects() {
        return List.of(
                subject("MATH", "Mathematiques", List.of(), List.of(), true, 1, false),
                subject("FRAN", "Francais", List.of(), List.of(), true, 2, false),
                subject("PHYS", "Physique", List.of("2NDE", "1ERE", "TLE"), List.of(), false, 3, false),
                subject("PHILO", "Philosophie", List.of("TLE"), List.of(), false, 4, false),
                subject("LATIN", "Latin", List.of(), List.of(), false, 5, true));
    }

    private static EducationLevel level(String code, String label, String cycle, int rank,
            int ageMin, int ageMax, boolean hasTracks, boolean archived) {
        return new EducationLevel(SYSTEM + ":" + code, SYSTEM, code, label, cycle, rank,
                ageMin, ageMax, hasTracks, archived);
    }

    private static Track track(String code, String label, String description,
            List<String> levelCodes, int order, boolean archived) {
        return new Track(SYSTEM + ":" + code, SYSTEM, code, label, description, levelCodes,
                order, archived);
    }

    private static Subject subject(String code, String label, List<String> levelCodes,
            List<String> trackCodes, boolean core, int order, boolean archived) {
        return new Subject(SYSTEM + ":" + code, SYSTEM, code, label, levelCodes, trackCodes,
                core, order, archived);
    }
}
