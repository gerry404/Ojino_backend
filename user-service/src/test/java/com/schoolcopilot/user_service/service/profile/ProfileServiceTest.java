package com.schoolcopilot.user_service.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.user_service.TestFixtures;
import com.schoolcopilot.user_service.domain.profile.AvailabilitySlot;
import com.schoolcopilot.user_service.domain.profile.Difficulty;
import com.schoolcopilot.user_service.domain.profile.Goal;
import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

class ProfileServiceTest {

    private static final String USER = "user-1";

    private final Map<String, StudentProfile> stored = new LinkedHashMap<>();
    private ProfileService profiles;
    private OnboardingService onboarding;

    @BeforeEach
    void setUp() {
        stored.clear();
        StudentProfileRepository repository = mock(StudentProfileRepository.class);
        when(repository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0);
            stored.put(profile.getId(), profile);
            return profile;
        });
        when(repository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(invocation.getArgument(0))));

        onboarding = new OnboardingService(TestFixtures.referenceService());
        profiles = new ProfileService(repository, TestFixtures.referenceService(), onboarding);
    }

    // ------------------------------------------------------------------
    // Identite
    // ------------------------------------------------------------------

    @Test
    @DisplayName("l'age se deduit de la date de naissance, il n'est jamais stocke")
    void ageIsDerivedFromBirthDate() {
        LocalDate birthDate = LocalDate.now().minusYears(16).minusDays(10);

        StudentProfile profile = profiles.updateIdentity(USER, " Paul ", " Martin ", birthDate);

        assertThat(profile.age()).isEqualTo(16);
        assertThat(profile.fullName()).isEqualTo("Paul Martin");
        assertThat(profile.hasCompleted(OnboardingStep.IDENTITY)).isTrue();
    }

    @Test
    @DisplayName("une date de naissance dans le futur est refusee")
    void futureBirthDateIsRejected() {
        assertThatThrownBy(() ->
                profiles.updateIdentity(USER, "Paul", "Martin", LocalDate.now().plusDays(1)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_birth_date");
    }

    @Test
    @DisplayName("un age invraisemblable est refuse")
    void implausibleAgeIsRejected() {
        assertThatThrownBy(() ->
                profiles.updateIdentity(USER, "Paul", "Martin", LocalDate.now().minusYears(3)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_birth_date");
    }

    // ------------------------------------------------------------------
    // Niveau et filiere
    // ------------------------------------------------------------------

    @Test
    @DisplayName("changer de niveau remet a zero filiere, matieres et difficultes")
    void changingLevelResetsWhatDependsOnIt() {
        completeUpTo(OnboardingStep.DIFFICULTIES);
        assertThat(stored.get(USER).getSubjectCodes()).isNotEmpty();

        StudentProfile profile = profiles.updateLevel(USER, TestFixtures.SYSTEM, "5E");

        // Garder des matieres de Terminale sur un profil de 5e produirait un profil
        // incoherent : mieux vaut redemander.
        assertThat(profile.getTrackCode()).isNull();
        assertThat(profile.getSubjectCodes()).isEmpty();
        assertThat(profile.getDifficulties()).isEmpty();
        assertThat(profile.hasCompleted(OnboardingStep.SUBJECTS)).isFalse();
        assertThat(profile.hasCompleted(OnboardingStep.DIFFICULTIES)).isFalse();
        assertThat(profile.hasCompleted(OnboardingStep.LEVEL)).isTrue();
    }

    @Test
    @DisplayName("reconfirmer le meme niveau ne detruit rien")
    void confirmingTheSameLevelKeepsEverything() {
        completeUpTo(OnboardingStep.SUBJECTS);
        List<String> before = List.copyOf(stored.get(USER).getSubjectCodes());

        StudentProfile profile = profiles.updateLevel(USER, TestFixtures.SYSTEM, "TLE");

        assertThat(profile.getSubjectCodes()).isEqualTo(before);
        assertThat(profile.getTrackCode()).isEqualTo("D");
    }

    @Test
    @DisplayName("choisir une filiere sur un niveau qui n'en a pas est refuse")
    void trackIsRefusedOnALevelWithoutTracks() {
        profiles.updateLevel(USER, TestFixtures.SYSTEM, "5E");

        assertThatThrownBy(() -> profiles.updateTrack(USER, "D"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "track_not_applicable");
    }

    @Test
    @DisplayName("choisir une filiere avant le niveau est refuse")
    void trackBeforeLevelIsRefused() {
        assertThatThrownBy(() -> profiles.updateTrack(USER, "D"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "step_out_of_order");
    }

    // ------------------------------------------------------------------
    // Matieres et difficultes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("une difficulte doit porter sur une matiere effectivement suivie")
    void difficultyMustTargetAChosenSubject() {
        completeUpTo(OnboardingStep.SUBJECTS);

        // PHYS existe bien en Terminale, mais cet eleve ne l'a pas choisie.
        assertThatThrownBy(() -> profiles.updateDifficulties(USER,
                List.of(new Difficulty("PHYS", 2, "matiere que je ne suis pas"))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "difficulty_on_unknown_subject");
    }

    @Test
    @DisplayName("ne declarer aucune difficulte est une reponse valable")
    void anEmptyDifficultyListCompletesTheStep() {
        completeUpTo(OnboardingStep.SUBJECTS);

        StudentProfile profile = profiles.updateDifficulties(USER, List.of());

        assertThat(profile.hasCompleted(OnboardingStep.DIFFICULTIES)).isTrue();
    }

    @Test
    @DisplayName("retirer une matiere retire la difficulte qui la visait")
    void removingASubjectDropsItsDifficulty() {
        completeUpTo(OnboardingStep.DIFFICULTIES);
        assertThat(stored.get(USER).getDifficulties()).isNotEmpty();

        StudentProfile profile = profiles.updateSubjects(USER, List.of("FRAN"));

        assertThat(profile.getDifficulties()).isEmpty();
    }

    @Test
    @DisplayName("les difficultes avant les matieres sont refusees")
    void difficultiesBeforeSubjectsAreRefused() {
        assertThatThrownBy(() -> profiles.updateDifficulties(USER, List.of()))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "step_out_of_order");
    }

    // ------------------------------------------------------------------
    // Disponibilites
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deux creneaux qui se chevauchent le meme jour sont refuses")
    void overlappingSlotsAreRejected() {
        assertThatThrownBy(() -> profiles.updateAvailability(USER, List.of(
                slot(DayOfWeek.MONDAY, "18:00", "19:30"),
                slot(DayOfWeek.MONDAY, "19:00", "20:00"))))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_availability");
    }

    @Test
    @DisplayName("les memes horaires sur deux jours differents sont acceptes")
    void sameHoursOnDifferentDaysAreFine() {
        StudentProfile profile = profiles.updateAvailability(USER, List.of(
                slot(DayOfWeek.MONDAY, "18:00", "19:30"),
                slot(DayOfWeek.TUESDAY, "18:00", "19:30")));

        assertThat(profile.weeklyMinutes()).isEqualTo(180);
    }

    @Test
    @DisplayName("un creneau qui finit avant de commencer est refuse")
    void invertedSlotIsRejected() {
        assertThatThrownBy(() -> profiles.updateAvailability(USER, List.of(
                slot(DayOfWeek.MONDAY, "20:00", "18:00"))))
                .isInstanceOf(ApiException.class);
    }

    // ------------------------------------------------------------------
    // Parcours complet
    // ------------------------------------------------------------------

    @Test
    @DisplayName("le parcours complet marque le profil comme termine")
    void completingEveryStepFinishesOnboarding() {
        completeUpTo(OnboardingStep.AVAILABILITY);

        StudentProfile profile = stored.get(USER);
        assertThat(onboarding.isComplete(profile)).isTrue();
        assertThat(profile.isOnboardingComplete()).isTrue();
        assertThat(onboarding.stateOf(profile).nextStep()).isNull();
    }

    @Test
    @DisplayName("revenir modifier une etape peut rouvrir le parcours")
    void reopeningAStepReopensOnboarding() {
        completeUpTo(OnboardingStep.AVAILABILITY);
        assertThat(stored.get(USER).isOnboardingComplete()).isTrue();

        // Repasser en 5e efface les matieres : le parcours n'est plus complet.
        StudentProfile profile = profiles.updateLevel(USER, TestFixtures.SYSTEM, "5E");

        assertThat(profile.isOnboardingComplete()).isFalse();
        assertThat(onboarding.stateOf(profile).nextStep()).isEqualTo(OnboardingStep.SUBJECTS);
    }

    @Test
    @DisplayName("un eleve de college ne se voit jamais demander de filiere")
    void collegeStudentsSkipTheTrackStep() {
        profiles.updateIdentity(USER, "Paul", "Martin", LocalDate.now().minusYears(13));
        profiles.skipPhoto(USER);
        StudentProfile profile = profiles.updateLevel(USER, TestFixtures.SYSTEM, "5E");

        OnboardingService.State state = onboarding.stateOf(profile);

        assertThat(state.steps().stream()
                .filter(step -> step.step() == OnboardingStep.TRACK)
                .findFirst().orElseThrow().applicable()).isFalse();
        assertThat(state.nextStep()).isEqualTo(OnboardingStep.SUBJECTS);
    }

    // ------------------------------------------------------------------

    /** Deroule le parcours jusqu'a l'etape indiquee, incluse. */
    private void completeUpTo(OnboardingStep last) {
        profiles.updateIdentity(USER, "Paul", "Martin", LocalDate.now().minusYears(17));
        if (last.order() < OnboardingStep.PHOTO.order()) {
            return;
        }
        profiles.skipPhoto(USER);
        profiles.updateLevel(USER, TestFixtures.SYSTEM, "TLE");
        if (last.order() < OnboardingStep.TRACK.order()) {
            return;
        }
        profiles.updateTrack(USER, "D");
        if (last.order() < OnboardingStep.SUBJECTS.order()) {
            return;
        }
        profiles.updateSubjects(USER, List.of("MATH", "FRAN", "PHILO"));
        if (last.order() < OnboardingStep.GOAL.order()) {
            return;
        }
        profiles.updateGoal(USER, Goal.PASS_EXAM, "Baccalaureat", null);
        if (last.order() < OnboardingStep.DIFFICULTIES.order()) {
            return;
        }
        profiles.updateDifficulties(USER, List.of(new Difficulty("MATH", 3, "les limites")));
        if (last.order() < OnboardingStep.AVAILABILITY.order()) {
            return;
        }
        profiles.updateAvailability(USER, List.of(slot(DayOfWeek.WEDNESDAY, "17:00", "19:00")));
    }

    private AvailabilitySlot slot(DayOfWeek day, String start, String end) {
        return new AvailabilitySlot(day, LocalTime.parse(start), LocalTime.parse(end));
    }
}
