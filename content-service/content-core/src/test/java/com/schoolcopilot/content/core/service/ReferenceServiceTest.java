package com.schoolcopilot.content.core.service;

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

import com.schoolcopilot.content.core.TestFixtures;
import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;
import com.schoolcopilot.content.core.service.ReferenceService.SuggestedLevel;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;

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

        reference = new ReferenceService(systems, levels, tracks, subjects,
                TestFixtures.curriculumModules());
    }

    // ------------------------------------------------------------------
    // Cycles
    // ------------------------------------------------------------------

    @Test
    @DisplayName("les cycles listes sont ceux du systeme dont le module est embarque")
    void cyclesAreThosePresentAndSupported() {
        // Le systeme contient aussi une licence, mais aucun module universitaire
        // n'est embarque dans les tests du coeur : elle ne doit pas apparaitre.
        assertThat(reference.cyclesOf(TestFixtures.SYSTEM))
                .extracting(CurriculumModule::cycle)
                .containsExactly(EducationCycle.EARLY_YEARS, EducationCycle.COLLEGE,
                        EducationCycle.HIGH_SCHOOL);
    }

    @Test
    @DisplayName("chaque cycle annonce les choix qu'il demande apres la classe")
    void eachCycleDeclaresItsSteps() {
        assertThat(reference.stepsFor(EducationCycle.HIGH_SCHOOL))
                .containsExactly(CurriculumStep.TRACK, CurriculumStep.SUBJECTS);
        assertThat(reference.stepsFor(EducationCycle.EARLY_YEARS))
                .containsExactly(CurriculumStep.LEARNING_DOMAINS);
        // Module absent : aucune etape, et surtout pas d'exception.
        assertThat(reference.stepsFor(EducationCycle.PREPA)).isEmpty();
    }

    @Test
    @DisplayName("un niveau dont le cycle n'est pas embarque ne peut pas etre choisi")
    void levelOfAnUnsupportedCycleIsRefused() {
        assertThatThrownBy(() -> reference.requireSelectableLevel(TestFixtures.SYSTEM, "L1"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "cycle_not_available");
    }

    // ------------------------------------------------------------------
    // Suggestion par l'age
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a 15 ans, 3e et Seconde sont suggerees, mais tous les niveaux restent proposes")
    void ageSuggestsWithoutFiltering() {
        List<SuggestedLevel> levels = reference.levelsFor(TestFixtures.SYSTEM, 15, null);

        assertThat(levels).hasSize(9);
        assertThat(suggestedCodes(levels)).containsExactly("3E", "2NDE");
    }

    @Test
    @DisplayName("sans age, aucun niveau n'est mis en avant")
    void noAgeMeansNoSuggestion() {
        assertThat(suggestedCodes(reference.levelsFor(TestFixtures.SYSTEM, null, null))).isEmpty();
    }

    @Test
    @DisplayName("un eleve de 25 ans qui reprend ses etudes se voit proposer la Terminale")
    void ageBeyondEveryRangeFallsBackToTheClosestLevel() {
        assertThat(suggestedCodes(reference.levelsFor(TestFixtures.SYSTEM, 25, null)))
                .containsExactly("TLE");
    }

    @Test
    @DisplayName("un niveau archive n'est jamais suggere, meme s'il colle exactement a l'age")
    void archivedLevelIsNeverSuggested() {
        // 1AC couvre 10-11 ans et serait la correspondance exacte, mais il est
        // archive : c'est la 6e, le plus proche encore propose, qui remonte.
        assertThat(suggestedCodes(reference.levelsFor(TestFixtures.SYSTEM, 10, null)))
                .containsExactly("6E");
    }

    @Test
    @DisplayName("on peut n'afficher qu'un seul cycle")
    void levelsCanBeFilteredByCycle() {
        assertThat(reference.levelsFor(TestFixtures.SYSTEM, null, EducationCycle.EARLY_YEARS))
                .extracting(suggested -> suggested.level().code())
                .containsExactly("GS", "CP");
    }

    @Test
    @DisplayName("un systeme inconnu est refuse")
    void unknownSystemIsRejected() {
        assertThatThrownBy(() -> reference.levelsFor("INCONNU", 15, null))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_system");
    }

    // ------------------------------------------------------------------
    // Archive : encore resolvable, plus choisissable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un niveau archive reste resolvable pour les profils qui le portent")
    void archivedLevelRemainsResolvable() {
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
    @DisplayName("un cycle qui ne demande pas de filiere n'en propose aucune")
    void cyclesWithoutTracksProposeNone() {
        assertThat(reference.tracksFor(TestFixtures.SYSTEM, "5E")).isEmpty();
        assertThat(reference.tracksFor(TestFixtures.SYSTEM, "CP")).isEmpty();
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
    @DisplayName("la maternelle ne se voit proposer aucune matiere")
    void earlyYearsHasNoSubjects() {
        // Les matieres sans restriction de niveau valent partout : sans ce
        // garde-fou, un enfant de grande section se verrait proposer la
        // philosophie. Son cycle ne demande pas de matieres, la liste est vide.
        assertThat(reference.subjectsFor(TestFixtures.SYSTEM, "GS", null)).isEmpty();
        assertThat(reference.subjectsFor(TestFixtures.SYSTEM, "CP", null)).isEmpty();
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
