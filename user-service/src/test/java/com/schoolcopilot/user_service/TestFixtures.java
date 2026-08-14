package com.schoolcopilot.user_service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.repository.ReferenceRepositories;
import com.schoolcopilot.user_service.service.reference.ReferenceService;

/** Un referentiel reduit, calque sur le systeme francophone camerounais. */
public final class TestFixtures {

    public static final String SYSTEM = "CM-FR";

    private TestFixtures() {
    }

    /** Un {@link ReferenceService} reel, adosse au referentiel reduit ci-dessous. */
    public static ReferenceService referenceService() {
        ReferenceRepositories.EducationSystems systems =
                mock(ReferenceRepositories.EducationSystems.class);
        ReferenceRepositories.EducationLevels levelRepo =
                mock(ReferenceRepositories.EducationLevels.class);
        ReferenceRepositories.Tracks trackRepo = mock(ReferenceRepositories.Tracks.class);
        ReferenceRepositories.Subjects subjectRepo = mock(ReferenceRepositories.Subjects.class);

        when(systems.findById(anyString())).thenAnswer(invocation ->
                SYSTEM.equals(invocation.getArgument(0))
                        ? Optional.of(system())
                        : Optional.empty());
        when(systems.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(system()));
        when(levelRepo.findBySystemCodeOrderByRankAsc(SYSTEM)).thenReturn(levels());
        when(levelRepo.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> levels().stream()
                        .filter(level -> level.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(trackRepo.findBySystemCodeOrderByDisplayOrderAsc(SYSTEM)).thenReturn(tracks());
        when(trackRepo.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> tracks().stream()
                        .filter(track -> track.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(subjectRepo.findBySystemCodeOrderByDisplayOrderAsc(SYSTEM)).thenReturn(subjects());

        return new ReferenceService(systems, levelRepo, trackRepo, subjectRepo);
    }

    public static EducationSystem system() {
        return new EducationSystem(SYSTEM, "CM", "Cameroun", "Systeme francophone", "fr", 1, true);
    }

    public static List<EducationLevel> levels() {
        return List.of(
                level("6E", "6e", "COLLEGE", 1, 11, 12, false),
                level("5E", "5e", "COLLEGE", 2, 12, 13, false),
                level("4E", "4e", "COLLEGE", 3, 13, 14, false),
                level("3E", "3e", "COLLEGE", 4, 14, 15, false),
                level("2NDE", "Seconde", "LYCEE", 5, 15, 16, true),
                level("1ERE", "Premiere", "LYCEE", 6, 16, 17, true),
                level("TLE", "Terminale", "LYCEE", 7, 17, 19, true));
    }

    public static List<Track> tracks() {
        return List.of(
                new Track(SYSTEM + ":SA", SYSTEM, "SA", "Seconde A", "Litteraire",
                        List.of("2NDE"), 1),
                new Track(SYSTEM + ":C", SYSTEM, "C", "C", "Maths et sciences physiques",
                        List.of("1ERE", "TLE"), 2),
                new Track(SYSTEM + ":D", SYSTEM, "D", "D", "Maths et sciences de la vie",
                        List.of("1ERE", "TLE"), 3));
    }

    public static List<Subject> subjects() {
        return List.of(
                subject("MATH", "Mathematiques", List.of(), List.of(), true, 1),
                subject("FRAN", "Francais", List.of(), List.of(), true, 2),
                subject("PHYS", "Physique", List.of("2NDE", "1ERE", "TLE"), List.of(), false, 3),
                subject("PHILO", "Philosophie", List.of("TLE"), List.of(), false, 4));
    }

    private static EducationLevel level(String code, String label, String cycle, int rank,
            int ageMin, int ageMax, boolean hasTracks) {
        return new EducationLevel(SYSTEM + ":" + code, SYSTEM, code, label, cycle, rank,
                ageMin, ageMax, hasTracks);
    }

    private static Subject subject(String code, String label, List<String> levelCodes,
            List<String> trackCodes, boolean core, int order) {
        return new Subject(SYSTEM + ":" + code, SYSTEM, code, label, levelCodes, trackCodes,
                core, order);
    }
}
