package com.schoolcopilot.content_service.service.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.content_service.TestFixtures;
import com.schoolcopilot.content_service.domain.reference.Subject;
import com.schoolcopilot.content_service.domain.reference.Track;
import com.schoolcopilot.content_service.exception.ApiException;
import com.schoolcopilot.content_service.repository.ReferenceRepositories;
import com.schoolcopilot.content_service.service.reference.ReferenceService.SuggestedLevel;

class ReferenceServiceTest {

    private ReferenceService reference;

    @BeforeEach
    void setUp() {
        ReferenceRepositories.EducationSystems systems =
                mock(ReferenceRepositories.EducationSystems.class);
        ReferenceRepositories.EducationLevels levels =
                mock(ReferenceRepositories.EducationLevels.class);
        ReferenceRepositories.Tracks tracks = mock(ReferenceRepositories.Tracks.class);
        ReferenceRepositories.Subjects subjects = mock(ReferenceRepositories.Subjects.class);

        when(systems.findById(TestFixtures.SYSTEM)).thenReturn(Optional.of(TestFixtures.system()));
        when(systems.findById("INCONNU")).thenReturn(Optional.empty());
        when(levels.findBySystemCodeOrderByRankAsc(TestFixtures.SYSTEM))
                .thenReturn(TestFixtures.levels());
        when(levels.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> TestFixtures.levels().stream()
                        .filter(level -> level.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(tracks.findBySystemCodeOrderByDisplayOrderAsc(TestFixtures.SYSTEM))
                .thenReturn(TestFixtures.tracks());
        when(tracks.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> TestFixtures.tracks().stream()
                        .filter(track -> track.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(subjects.findBySystemCodeOrderByDisplayOrderAsc(TestFixtures.SYSTEM))
                .thenReturn(TestFixtures.subjects());

        reference = new ReferenceService(systems, levels, tracks, subjects);
    }

    // ------------------------------------------------------------------
    // Suggestion par l'age
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a 15 ans, 3e et Seconde sont suggerees, mais tous les niveaux restent proposes")
    void ageSuggestsWithoutFiltering() {
        List<SuggestedLevel> levels = reference.levelsFor(TestFixtures.SYSTEM, 15);

        assertThat(levels).hasSize(7);
        assertThat(suggestedCodes(levels)).containsExactly("3E", "2NDE");
    }

    @Test
    @DisplayName("sans age, aucun niveau n'est mis en avant")
    void noAgeMeansNoSuggestion() {
        List<SuggestedLevel> levels = reference.levelsFor(TestFixtures.SYSTEM, null);

        assertThat(levels).hasSize(7);
        assertThat(suggestedCodes(levels)).isEmpty();
    }

    @Test
    @DisplayName("un eleve de 25 ans qui reprend ses etudes se voit proposer la Terminale")
    void ageBeyondEveryRangeFallsBackToTheClosestLevel() {
        assertThat(suggestedCodes(reference.levelsFor(TestFixtures.SYSTEM, 25)))
                .containsExactly("TLE");
    }

    @Test
    @DisplayName("un niveau archive n'est jamais suggere, meme s'il colle mieux a l'age")
    void archivedLevelIsNeverSuggested() {
        // 1AC couvre 10-11 ans et serait le plus proche pour un enfant de 8 ans,
        // mais il est archive : c'est la 6e qui doit remonter.
        assertThat(suggestedCodes(reference.levelsFor(TestFixtures.SYSTEM, 8)))
                .containsExactly("6E");
    }

    @Test
    @DisplayName("les niveaux archives disparaissent de la liste proposee")
    void archivedLevelsAreHidden() {
        assertThat(reference.levelsFor(TestFixtures.SYSTEM, null))
                .extracting(suggested -> suggested.level().code())
                .doesNotContain("1AC");
    }

    @Test
    @DisplayName("un systeme inconnu est refuse")
    void unknownSystemIsRejected() {
        assertThatThrownBy(() -> reference.levelsFor("INCONNU", 15))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_system");
    }

    // ------------------------------------------------------------------
    // Archive : encore resolvable, plus choisissable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un niveau archive reste resolvable pour les profils qui le portent")
    void archivedLevelRemainsResolvable() {
        // C'est toute la raison d'archiver plutot que de supprimer.
        assertThat(reference.findLevel(TestFixtures.SYSTEM, "1AC").label())
                .isEqualTo("1re annee (ancien)");
    }

    @Test
    @DisplayName("un niveau archive ne peut plus etre choisi")
    void archivedLevelCannotBeSelected() {
        assertThatThrownBy(() -> reference.requireSelectableLevel(TestFixtures.SYSTEM, "1AC"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "archived");
    }

    @Test
    @DisplayName("une filiere archivee ne peut plus etre choisie")
    void archivedTrackCannotBeSelected() {
        assertThatThrownBy(() -> reference.requireSelectableTrack(TestFixtures.SYSTEM, "TLE", "E"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "archived");
    }

    @Test
    @DisplayName("un niveau inconnu est refuse")
    void unknownLevelIsRejected() {
        assertThatThrownBy(() -> reference.findLevel(TestFixtures.SYSTEM, "XYZ"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_level");
    }

    // ------------------------------------------------------------------
    // Filieres
    // ------------------------------------------------------------------

    @Test
    @DisplayName("la Seconde et la Terminale n'ont pas les memes filieres")
    void tracksDependOnTheLevel() {
        assertThat(reference.tracksFor(TestFixtures.SYSTEM, "2NDE").stream().map(Track::code))
                .containsExactly("SA");
        assertThat(reference.tracksFor(TestFixtures.SYSTEM, "TLE").stream().map(Track::code))
                .containsExactly("C", "D");
    }

    @Test
    @DisplayName("un niveau de college n'a aucune filiere")
    void collegeLevelsHaveNoTracks() {
        assertThat(reference.tracksFor(TestFixtures.SYSTEM, "5E")).isEmpty();
    }

    @Test
    @DisplayName("une filiere qui n'existe pas a ce niveau est refusee")
    void trackMustExistAtThatLevel() {
        assertThatThrownBy(() -> reference.requireSelectableTrack(TestFixtures.SYSTEM, "2NDE", "D"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "track_not_available");
    }

    // ------------------------------------------------------------------
    // Matieres
    // ------------------------------------------------------------------

    @Test
    @DisplayName("la philosophie n'apparait qu'en Terminale")
    void subjectsAreRestrictedByLevel() {
        assertThat(reference.subjectsFor(TestFixtures.SYSTEM, "5E", null).stream()
                .map(Subject::code))
                .containsExactly("MATH", "FRAN");

        assertThat(reference.subjectsFor(TestFixtures.SYSTEM, "TLE", "D").stream()
                .map(Subject::code))
                .containsExactly("MATH", "FRAN", "PHYS", "PHILO");
    }

    @Test
    @DisplayName("une matiere archivee n'est plus proposee")
    void archivedSubjectIsHidden() {
        assertThat(reference.subjectsFor(TestFixtures.SYSTEM, "TLE", "D").stream()
                .map(Subject::code))
                .doesNotContain("LATIN");
    }

    @Test
    @DisplayName("choisir une matiere qui n'existe pas a ce niveau est refuse")
    void subjectsAreValidatedAgainstTheLevel() {
        assertThatThrownBy(() ->
                reference.validateSubjects(TestFixtures.SYSTEM, "5E", null, List.of("MATH", "PHILO")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_subjects");
    }

    @Test
    @DisplayName("choisir une matiere archivee est refuse")
    void archivedSubjectCannotBeSelected() {
        assertThatThrownBy(() ->
                reference.validateSubjects(TestFixtures.SYSTEM, "TLE", "D", List.of("MATH", "LATIN")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_subjects");
    }

    @Test
    @DisplayName("les doublons de la selection sont ecartes")
    void duplicateSubjectsAreRemoved() {
        assertThat(reference.validateSubjects(
                TestFixtures.SYSTEM, "TLE", "D", List.of("MATH", "FRAN", "MATH")))
                .containsExactly("MATH", "FRAN");
    }

    private List<String> suggestedCodes(List<SuggestedLevel> levels) {
        return levels.stream()
                .filter(SuggestedLevel::suggested)
                .map(suggested -> suggested.level().code())
                .toList();
    }
}
