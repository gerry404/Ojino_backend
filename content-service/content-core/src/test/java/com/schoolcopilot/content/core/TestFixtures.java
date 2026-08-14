package com.schoolcopilot.content.core;

import java.util.List;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.EducationSystem;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumModules;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/** Un referentiel reduit, calque sur le systeme francophone camerounais. */
public final class TestFixtures {

    public static final String SYSTEM = "CM-FR";

    private TestFixtures() {
    }

    /**
     * Les trois cycles utiles aux tests du coeur. Le coeur ne connait aucun module
     * reel : il ne voit que des implementations de {@link CurriculumModule}.
     */
    public static CurriculumModules curriculumModules() {
        return new CurriculumModules(List.of(
                module(EducationCycle.EARLY_YEARS, "Maternelle et CP",
                        List.of(CurriculumStep.LEARNING_DOMAINS)),
                module(EducationCycle.COLLEGE, "Collège",
                        List.of(CurriculumStep.SUBJECTS)),
                module(EducationCycle.HIGH_SCHOOL, "Lycée",
                        List.of(CurriculumStep.TRACK, CurriculumStep.SUBJECTS))));
    }

    public static EducationSystem system() {
        return new EducationSystem(SYSTEM, "CM", "Cameroun", "Systeme francophone", "fr", 1, true);
    }

    public static List<EducationLevel> levels() {
        return List.of(
                level("GS", "Grande section", EducationCycle.EARLY_YEARS, 3, 5, 6, false, false),
                level("CP", "CP", EducationCycle.EARLY_YEARS, 4, 6, 7, false, false),
                level("6E", "6e", EducationCycle.COLLEGE, 10, 11, 12, false, false),
                level("5E", "5e", EducationCycle.COLLEGE, 11, 12, 13, false, false),
                level("4E", "4e", EducationCycle.COLLEGE, 12, 13, 14, false, false),
                level("3E", "3e", EducationCycle.COLLEGE, 13, 14, 15, false, false),
                level("2NDE", "Seconde", EducationCycle.HIGH_SCHOOL, 20, 15, 16, true, false),
                level("1ERE", "Premiere", EducationCycle.HIGH_SCHOOL, 21, 16, 17, true, false),
                level("TLE", "Terminale", EducationCycle.HIGH_SCHOOL, 22, 17, 19, true, false),
                // Ancienne classe retiree : plus proposee, mais toujours resolvable
                // pour les profils qui la portent.
                level("1AC", "1re annee (ancien)", EducationCycle.COLLEGE, 9, 10, 11, false, true),
                // Cycle dont aucun module n'est embarque dans les tests du coeur.
                level("L1", "Licence 1", EducationCycle.UNIVERSITY, 30, 18, 25, false, false));
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

    private static CurriculumModule module(EducationCycle cycle, String label,
            List<CurriculumStep> steps) {
        return new CurriculumModule() {

            @Override
            public EducationCycle cycle() {
                return cycle;
            }

            @Override
            public String label() {
                return label;
            }

            @Override
            public List<CurriculumStep> steps() {
                return steps;
            }
        };
    }

    private static EducationLevel level(String code, String label, EducationCycle cycle, int rank,
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
