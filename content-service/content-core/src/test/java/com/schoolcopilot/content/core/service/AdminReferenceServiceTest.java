package com.schoolcopilot.content.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.content.core.TestFixtures;
import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.EducationSystem;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;

class AdminReferenceServiceTest {

    private final Map<String, EducationSystem> storedSystems = new LinkedHashMap<>();

    private ReferenceRepositories.EducationSystems systems;
    private ReferenceRepositories.EducationLevels levels;
    private ReferenceRepositories.Tracks tracks;
    private ReferenceRepositories.Subjects subjects;
    private AdminReferenceService admin;

    @BeforeEach
    void setUp() {
        storedSystems.clear();
        storedSystems.put(TestFixtures.SYSTEM, TestFixtures.system());

        systems = mock(ReferenceRepositories.EducationSystems.class);
        levels = mock(ReferenceRepositories.EducationLevels.class);
        tracks = mock(ReferenceRepositories.Tracks.class);
        subjects = mock(ReferenceRepositories.Subjects.class);

        when(systems.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(storedSystems.get(invocation.getArgument(0))));
        when(systems.existsById(anyString())).thenAnswer(invocation ->
                storedSystems.containsKey(invocation.getArgument(0)));
        when(systems.save(any(EducationSystem.class))).thenAnswer(invocation -> {
            EducationSystem system = invocation.getArgument(0);
            storedSystems.put(system.code(), system);
            return system;
        });

        when(levels.findBySystemCodeOrderByRankAsc(TestFixtures.SYSTEM))
                .thenReturn(TestFixtures.levels());
        when(levels.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> TestFixtures.levels().stream()
                        .filter(level -> level.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(levels.save(any(EducationLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tracks.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> TestFixtures.tracks().stream()
                        .filter(track -> track.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(tracks.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));

        when(subjects.findBySystemCodeAndCode(anyString(), anyString()))
                .thenAnswer(invocation -> TestFixtures.subjects().stream()
                        .filter(subject -> subject.code().equals(invocation.getArgument(1)))
                        .findFirst());
        when(subjects.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        admin = new AdminReferenceService(systems, levels, tracks, subjects);
    }

    // ------------------------------------------------------------------
    // Systemes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le code d'un nouveau systeme est normalise en majuscules")
    void systemCodeIsNormalized() {
        EducationSystem created = admin.createSystem(new EducationSystem(" cm-en ", "CM",
                "Cameroun", "Anglophone", "en", 2, true));

        assertThat(created.code()).isEqualTo("CM-EN");
    }

    @Test
    @DisplayName("creer un systeme deja existant est refuse")
    void duplicateSystemIsRejected() {
        assertThatThrownBy(() -> admin.createSystem(TestFixtures.system()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "already_exists");
    }

    @Test
    @DisplayName("un systeme se desactive au lieu de se supprimer")
    void systemsAreDeactivatedNotDeleted() {
        EducationSystem deactivated = admin.setSystemActive(TestFixtures.SYSTEM, false);

        assertThat(deactivated.active()).isFalse();
        assertThat(storedSystems).containsKey(TestFixtures.SYSTEM);
    }

    @Test
    @DisplayName("agir sur un systeme inconnu est refuse")
    void unknownSystemIsRejected() {
        assertThatThrownBy(() -> admin.listLevels("INCONNU"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_system");
    }

    // ------------------------------------------------------------------
    // Niveaux
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le back-office voit aussi les niveaux archives")
    void adminSeesArchivedLevels() {
        // La route publique les masque ; celle-ci doit permettre de les desarchiver.
        assertThat(admin.listLevels(TestFixtures.SYSTEM))
                .extracting(EducationLevel::code)
                .contains("1AC");
    }

    @Test
    @DisplayName("une tranche d'age a l'envers est refusee")
    void invertedAgeRangeIsRejected() {
        assertThatThrownBy(() -> admin.createLevel(TestFixtures.SYSTEM,
                new EducationLevel(null, null, "XX", "Test", EducationCycle.COLLEGE, 1, 18, 12, false, false)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_age_range");
    }

    @Test
    @DisplayName("un niveau cree recoit un identifiant deterministe et n'est pas archive")
    void createdLevelGetsADeterministicId() {
        EducationLevel created = admin.createLevel(TestFixtures.SYSTEM,
                new EducationLevel(null, null, "1AS", "1re annee", EducationCycle.COLLEGE, 1, 11, 12, false, false));

        assertThat(created.id()).isEqualTo("CM-FR:1AS");
        assertThat(created.systemCode()).isEqualTo(TestFixtures.SYSTEM);
        assertThat(created.archived()).isFalse();
    }

    @Test
    @DisplayName("creer un niveau deja existant est refuse")
    void duplicateLevelIsRejected() {
        assertThatThrownBy(() -> admin.createLevel(TestFixtures.SYSTEM,
                new EducationLevel(null, null, "TLE", "Terminale", EducationCycle.HIGH_SCHOOL, 7, 17, 19, true, false)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "already_exists");
    }

    @Test
    @DisplayName("modifier un niveau conserve son identifiant et son etat d'archive")
    void updatingALevelKeepsItsIdAndArchivedFlag() {
        EducationLevel updated = admin.updateLevel(TestFixtures.SYSTEM, "1AC",
                new EducationLevel(null, null, "1AC", "Renomme", EducationCycle.COLLEGE, 0, 10, 11, false, false));

        // L'archivage se pilote par sa propre route, pas en glissant un booleen
        // dans une modification de libelle.
        assertThat(updated.id()).isEqualTo("CM-FR:1AC");
        assertThat(updated.label()).isEqualTo("Renomme");
        assertThat(updated.archived()).isTrue();
    }

    @Test
    @DisplayName("archiver un niveau le retire des choix sans le supprimer")
    void levelCanBeArchived() {
        EducationLevel archived = admin.setLevelArchived(TestFixtures.SYSTEM, "TLE", true);

        assertThat(archived.archived()).isTrue();
        assertThat(archived.id()).isEqualTo("CM-FR:TLE");
        assertThat(archived.label()).isEqualTo("Terminale");
    }

    @Test
    @DisplayName("un niveau archive par erreur se desarchive")
    void levelCanBeUnarchived() {
        assertThat(admin.setLevelArchived(TestFixtures.SYSTEM, "1AC", false).archived()).isFalse();
    }

    @Test
    @DisplayName("archiver un niveau inconnu est refuse")
    void archivingUnknownLevelIsRejected() {
        assertThatThrownBy(() -> admin.setLevelArchived(TestFixtures.SYSTEM, "XYZ", true))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_level");
    }

    // ------------------------------------------------------------------
    // Filieres et matieres
    // ------------------------------------------------------------------

    @Test
    @DisplayName("une filiere rattachee a un niveau inexistant est refusee")
    void trackReferencingUnknownLevelIsRejected() {
        // Sans ce controle, la filiere existerait en base sans jamais apparaitre
        // nulle part, et personne ne comprendrait pourquoi.
        assertThatThrownBy(() -> admin.createTrack(TestFixtures.SYSTEM,
                new Track(null, null, "F", "Filiere F", "", List.of("TERMINALE_X"), 9, false)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_levels_referenced");
    }

    @Test
    @DisplayName("une filiere sur des niveaux connus est acceptee")
    void trackOnKnownLevelsIsAccepted() {
        Track created = admin.createTrack(TestFixtures.SYSTEM,
                new Track(null, null, "TI", "TI", "Technologies", List.of("1ERE", "TLE"), 8, false));

        assertThat(created.id()).isEqualTo("CM-FR:TI");
        assertThat(created.levelCodes()).containsExactly("1ERE", "TLE");
        assertThat(created.archived()).isFalse();
    }

    @Test
    @DisplayName("archiver une filiere la retire des choix sans la supprimer")
    void trackCanBeArchived() {
        Track archived = admin.setTrackArchived(TestFixtures.SYSTEM, "D", true);

        assertThat(archived.archived()).isTrue();
        assertThat(archived.label()).isEqualTo("D");
    }

    @Test
    @DisplayName("archiver une matiere la retire des choix sans la supprimer")
    void subjectCanBeArchived() {
        Subject archived = admin.setSubjectArchived(TestFixtures.SYSTEM, "MATH", true);

        assertThat(archived.archived()).isTrue();
        assertThat(archived.label()).isEqualTo("Mathematiques");
    }

    @Test
    @DisplayName("une matiere sans restriction vaut pour tout le systeme")
    void subjectWithoutRestrictionIsAccepted() {
        Subject created = admin.createSubject(TestFixtures.SYSTEM,
                new Subject(null, null, "EPS", "Sport", List.of(), List.of(), false, 20, false));

        assertThat(created.id()).isEqualTo("CM-FR:EPS");
        assertThat(created.appliesTo("6E", null)).isTrue();
        assertThat(created.appliesTo("TLE", "D")).isTrue();
    }
}
