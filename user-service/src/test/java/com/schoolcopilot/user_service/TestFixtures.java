package com.schoolcopilot.user_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.schoolcopilot.user_service.client.ContentClient;
import com.schoolcopilot.user_service.exception.ApiException;

/**
 * Une doublure de {@link ContentClient} adossee a un referentiel reduit, calque
 * sur le systeme francophone camerounais.
 *
 * <p>Le vrai referentiel vit dans content-service : ces tests verifient donc
 * comment user-service <em>reagit</em> a ses reponses, pas le referentiel
 * lui-meme, qui a ses propres tests chez lui.
 */
public final class TestFixtures {

    public static final String SYSTEM = "CM-FR";

    private TestFixtures() {
    }

    public static ContentClient contentClient() {
        ContentClient content = mock(ContentClient.class);

        when(content.requireSelectableLevel(anyString(), anyString()))
                .thenAnswer(invocation -> level(invocation.getArgument(1)));

        when(content.tracksFor(anyString(), anyString()))
                .thenAnswer(invocation -> tracksFor(invocation.getArgument(1)));

        when(content.subjectsFor(anyString(), anyString(), any()))
                .thenAnswer(invocation -> subjectsFor(invocation.getArgument(1)));

        when(content.learningDomainsFor(anyString(), anyString())).thenReturn(List.of(
                new ContentClient.LearningDomainView("LANGAGE", "Mobiliser le langage", null),
                new ContentClient.LearningDomainView("PENSEE", "Structurer sa pensee", null)));

        when(content.programsFor(anyString())).thenReturn(List.of(
                new ContentClient.ProgramView("INFO-L", "Licence informatique", "LICENCE",
                        "Sciences", 6)));

        when(content.courseUnitsFor(anyString(), anyString(), any())).thenReturn(List.of(
                new ContentClient.CourseUnitView("ALGO1", "Algorithmique 1", 1, 6, true),
                new ContentClient.CourseUnitView("MATH1", "Mathematiques 1", 1, 6, true)));

        return content;
    }

    /**
     * Un niveau par cycle, chacun avec la sequence que content-service annonce :
     * le college demande des matieres, le lycee filiere puis matieres, la
     * maternelle des domaines, le superieur parcours, semestre et unites.
     */
    private static ContentClient.LevelView level(String code) {
        return switch (code) {
            case "GS" -> new ContentClient.LevelView("GS", "Grande section", "EARLY_YEARS",
                    List.of("LEARNING_DOMAINS"), 3, 5, 6, false, false);
            case "5E" -> new ContentClient.LevelView("5E", "5e", "COLLEGE",
                    List.of("SUBJECTS"), 11, 12, 13, false, false);
            case "TLE" -> new ContentClient.LevelView("TLE", "Terminale", "HIGH_SCHOOL",
                    List.of("TRACK", "SUBJECTS"), 22, 17, 19, true, false);
            case "L1" -> new ContentClient.LevelView("L1", "Licence 1", "UNIVERSITY",
                    List.of("PROGRAM", "SEMESTER", "COURSE_UNITS"), 30, 18, 25, false, false);
            default -> throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "unknown_reference", "Le niveau " + code + " est inconnu du referentiel.");
        };
    }

    private static List<ContentClient.TrackView> tracksFor(String levelCode) {
        return switch (levelCode) {
            case "TLE", "1ERE" -> List.of(
                    new ContentClient.TrackView("C", "C", "Maths et sciences physiques"),
                    new ContentClient.TrackView("D", "D", "Maths et sciences de la vie"));
            case "2NDE" -> List.of(new ContentClient.TrackView("SA", "Seconde A", "Litteraire"));
            default -> List.of();
        };
    }

    private static List<ContentClient.SubjectView> subjectsFor(String levelCode) {
        List<ContentClient.SubjectView> common = List.of(
                new ContentClient.SubjectView("MATH", "Mathematiques", true),
                new ContentClient.SubjectView("FRAN", "Francais", true));

        if ("TLE".equals(levelCode)) {
            return List.of(
                    common.get(0),
                    common.get(1),
                    new ContentClient.SubjectView("PHYS", "Physique", false),
                    new ContentClient.SubjectView("PHILO", "Philosophie", false));
        }
        return common;
    }
}
