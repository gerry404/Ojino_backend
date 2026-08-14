package com.schoolcopilot.user_service.service.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.user_service.TestFixtures;
import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.ReferenceRepositories;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

class AdminReferenceServiceTest {

    private final Map<String, EducationSystem> storedSystems = new LinkedHashMap<>();

    private ReferenceRepositories.EducationSystems systems;
    private ReferenceRepositories.EducationLevels levels;
    private ReferenceRepositories.Tracks tracks;
    private ReferenceRepositories.Subjects subjects;
    private StudentProfileRepository profiles;
    private AdminReferenceService admin;

    @BeforeEach
    void setUp() {
        storedSystems.clear();
        storedSystems.put(TestFixtures.SYSTEM, TestFixtures.system());

        systems = mock(ReferenceRepositories.EducationSystems.class);
        levels = mock(ReferenceRepositories.EducationLevels.class);
        tracks = mock(ReferenceRepositories.Tracks.class);
        subjects = mock(ReferenceRepositories.Subjects.class);
        profiles = mock(StudentProfileRepository.class);

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

        admin = new AdminReferenceService(systems, levels, tracks, subjects, profiles);
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

        // Les profils rattaches doivent rester lisibles : il n'existe donc aucune
        // suppression de systeme dans l'API.
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
    @DisplayName("une tranche d'age a l'envers est refusee")
    void invertedAgeRangeIsRejected() {
        assertThatThrownBy(() -> admin.createLevel(TestFixtures.SYSTEM,
                new EducationLevel(null, null, "1E", "Premiere annee", "COLLEGE", 1, 18, 12, false)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_age_range");
    }

    @Test
    @DisplayName("un niveau cree recoit un identifiant deterministe")
    void createdLevelGetsADeterministicId() {
        EducationLevel created = admin.createLevel(TestFixtures.SYSTEM,
                new EducationLevel(null, null, "1AC", "1re annee", "COLLEGE", 1, 11, 12, false));

        assertThat(created.id()).isEqualTo("CM-FR:1AC");
        assertThat(created.systemCode()).isEqualTo(TestFixtures.SYSTEM);
    }

    @Test
    @DisplayName("modifier un niveau conserve son identifiant")
    void updatingALevelKeepsItsId() {
        EducationLevel updated = admin.updateLevel(TestFixtures.SYSTEM, "TLE",
                new EducationLevel(null, null, "TLE", "Terminale", "LYCEE", 7, 17, 20, true));

        assertThat(updated.id()).isEqualTo("CM-FR:TLE");
        assertThat(updated.typicalAgeMax()).isEqualTo(20);
    }

    @Test
    @DisplayName("un niveau encore utilise par des profils ne peut pas etre supprime")
    void levelInUseCannotBeDeleted() {
        when(profiles.countBySystemCodeAndLevelCode(TestFixtures.SYSTEM, "TLE")).thenReturn(42L);

        assertThatThrownBy(() -> admin.deleteLevel(TestFixtures.SYSTEM, "TLE"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "reference_in_use")
                .hasMessageContaining("42");

        verify(levels, never()).delete(any());
    }

    @Test
    @DisplayName("un niveau inutilise se supprime")
    void unusedLevelIsDeleted() {
        when(profiles.countBySystemCodeAndLevelCode(TestFixtures.SYSTEM, "TLE")).thenReturn(0L);

        admin.deleteLevel(TestFixtures.SYSTEM, "TLE");

        verify(levels).delete(any(EducationLevel.class));
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
                new Track(null, null, "F", "Filiere F", "", List.of("TERMINALE_X"), 9)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_levels_referenced");
    }

    @Test
    @DisplayName("une filiere sur des niveaux connus est acceptee")
    void trackOnKnownLevelsIsAccepted() {
        Track created = admin.createTrack(TestFixtures.SYSTEM,
                new Track(null, null, "TI", "TI", "Technologies", List.of("1ERE", "TLE"), 8));

        assertThat(created.id()).isEqualTo("CM-FR:TI");
        assertThat(created.levelCodes()).containsExactly("1ERE", "TLE");
    }

    @Test
    @DisplayName("une filiere encore choisie par des profils ne peut pas etre supprimee")
    void trackInUseCannotBeDeleted() {
        when(profiles.countBySystemCodeAndTrackCode(TestFixtures.SYSTEM, "D")).thenReturn(7L);

        assertThatThrownBy(() -> admin.deleteTrack(TestFixtures.SYSTEM, "D"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "reference_in_use");

        verify(tracks, never()).delete(any());
    }

    @Test
    @DisplayName("une matiere encore suivie ne peut pas etre supprimee")
    void subjectInUseCannotBeDeleted() {
        when(profiles.countBySystemCodeAndSubjectCodesContaining(TestFixtures.SYSTEM, "MATH"))
                .thenReturn(120L);

        assertThatThrownBy(() -> admin.deleteSubject(TestFixtures.SYSTEM, "MATH"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "reference_in_use");

        verify(subjects, never()).delete(any());
    }

    @Test
    @DisplayName("une matiere sans restriction vaut pour tout le systeme")
    void subjectWithoutRestrictionIsAccepted() {
        Subject created = admin.createSubject(TestFixtures.SYSTEM,
                new Subject(null, null, "EPS", "Sport", List.of(), List.of(), false, 20));

        assertThat(created.id()).isEqualTo("CM-FR:EPS");
        assertThat(created.appliesTo("6E", null)).isTrue();
        assertThat(created.appliesTo("TLE", "D")).isTrue();
    }
}
