package com.schoolcopilot.user_service.service.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.client.ContentClient;
import com.schoolcopilot.user_service.domain.profile.AvailabilitySlot;
import com.schoolcopilot.user_service.domain.profile.Difficulty;
import com.schoolcopilot.user_service.domain.profile.Goal;
import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

/**
 * Le profil scolaire, construit etape par etape.
 *
 * <p>Chaque etape s'enregistre seule. Quelqu'un qui abandonne au milieu du
 * parcours retrouve exactement ou il en etait, et peut revenir modifier une etape
 * deja validee sans repasser par les autres.
 *
 * <p>Ce service ne detient pas le referentiel scolaire : il ne stocke que les
 * codes choisis et demande a {@code content-service} de les valider. Ces appels
 * n'ont lieu que sur les ecritures — huit fois dans la vie d'un compte.
 */
@Service
public class ProfileService {

    private static final int MIN_AGE = 3;
    private static final int MAX_AGE = 100;

    private final StudentProfileRepository profiles;
    private final ContentClient content;
    private final OnboardingService onboarding;

    public ProfileService(StudentProfileRepository profiles, ContentClient content,
            OnboardingService onboarding) {
        this.profiles = profiles;
        this.content = content;
        this.onboarding = onboarding;
    }

    public StudentProfile getOrCreate(String userId) {
        return profiles.findById(userId).orElseGet(() -> StudentProfile.forUser(userId));
    }

    public StudentProfile require(String userId) {
        return profiles.findById(userId).orElseThrow(ApiException::profileNotFound);
    }

    // ------------------------------------------------------------------
    // Etapes
    // ------------------------------------------------------------------

    public StudentProfile updateIdentity(String userId, String firstName, String lastName,
            LocalDate birthDate) {
        validateBirthDate(birthDate);

        StudentProfile profile = getOrCreate(userId);
        profile.setFirstName(firstName.trim());
        profile.setLastName(lastName.trim());
        profile.setBirthDate(birthDate);
        profile.markCompleted(OnboardingStep.IDENTITY);
        return save(profile);
    }

    public StudentProfile updatePhoto(String userId, String avatarUrl) {
        StudentProfile profile = getOrCreate(userId);
        profile.setAvatarUrl(avatarUrl);
        profile.markCompleted(OnboardingStep.PHOTO);
        return save(profile);
    }

    /** L'etape photo est toujours passable : elle ne doit bloquer personne. */
    public StudentProfile skipPhoto(String userId) {
        StudentProfile profile = getOrCreate(userId);
        profile.markCompleted(OnboardingStep.PHOTO);
        return save(profile);
    }

    public StudentProfile updateLevel(String userId, String systemCode, String levelCode) {
        ContentClient.LevelView level = content.requireSelectableLevel(systemCode, levelCode);

        StudentProfile profile = getOrCreate(userId);
        boolean levelChanged = !Objects.equals(profile.getSystemCode(), systemCode)
                || !Objects.equals(profile.getLevelCode(), levelCode);

        profile.setSystemCode(systemCode);
        profile.setLevelCode(levelCode);
        // Recopie ici pour que la lecture de l'etat du parcours n'ait plus jamais
        // besoin d'interroger content-service.
        profile.setCurriculumSteps(toSteps(level.steps()));

        if (levelChanged) {
            // Tout ce qui depend du cycle devient caduc. Garder des matieres de
            // Terminale sur un profil de maternelle produirait un profil incoherent.
            profile.clearCurriculumChoices();
        }

        profile.markCompleted(OnboardingStep.LEVEL);
        return save(profile);
    }

    public StudentProfile updateTrack(String userId, String trackCode) {
        StudentProfile profile = requireStep(userId, OnboardingStep.TRACK);

        List<String> available = content
                .tracksFor(profile.getSystemCode(), profile.getLevelCode()).stream()
                .map(ContentClient.TrackView::code)
                .toList();
        if (!available.contains(trackCode)) {
            throw ApiException.trackNotAvailable(trackCode, profile.getLevelCode());
        }

        if (!Objects.equals(profile.getTrackCode(), trackCode)) {
            // Les matieres dependent aussi de la filiere.
            profile.setSubjectCodes(new ArrayList<>());
            profile.setDifficulties(new ArrayList<>());
            profile.getCompletedSteps().remove(OnboardingStep.SUBJECTS);
            profile.getCompletedSteps().remove(OnboardingStep.DIFFICULTIES);
        }

        profile.setTrackCode(trackCode);
        profile.markCompleted(OnboardingStep.TRACK);
        return save(profile);
    }

    public StudentProfile updateSubjects(String userId, List<String> subjectCodes) {
        StudentProfile profile = requireStep(userId, OnboardingStep.SUBJECTS);

        Set<String> allowed = content
                .subjectsFor(profile.getSystemCode(), profile.getLevelCode(), profile.getTrackCode())
                .stream()
                .map(ContentClient.SubjectView::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> validated = validateSelection(subjectCodes, allowed, "matiere");
        profile.setSubjectCodes(new ArrayList<>(validated));
        dropOrphanDifficulties(profile);

        profile.markCompleted(OnboardingStep.SUBJECTS);
        return save(profile);
    }

    /** Maternelle et CP : a la place des matieres. */
    public StudentProfile updateLearningDomains(String userId, List<String> domainCodes) {
        StudentProfile profile = requireStep(userId, OnboardingStep.LEARNING_DOMAINS);

        Set<String> allowed = content
                .learningDomainsFor(profile.getSystemCode(), profile.getLevelCode()).stream()
                .map(ContentClient.LearningDomainView::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> validated = validateSelection(domainCodes, allowed, "domaine");
        profile.setLearningDomainCodes(new ArrayList<>(validated));
        dropOrphanDifficulties(profile);

        profile.markCompleted(OnboardingStep.LEARNING_DOMAINS);
        return save(profile);
    }

    /** Superieur : le parcours suivi. */
    public StudentProfile updateProgram(String userId, String programCode) {
        StudentProfile profile = requireStep(userId, OnboardingStep.PROGRAM);

        boolean known = content.programsFor(profile.getSystemCode()).stream()
                .anyMatch(program -> program.code().equals(programCode));
        if (!known) {
            throw ApiException.unknownProgram(programCode);
        }

        if (!Objects.equals(profile.getProgramCode(), programCode)) {
            // Semestre et unites d'enseignement appartiennent au parcours.
            profile.setSemester(null);
            profile.setCourseUnitCodes(new ArrayList<>());
            profile.getCompletedSteps().remove(OnboardingStep.SEMESTER);
            profile.getCompletedSteps().remove(OnboardingStep.COURSE_UNITS);
            profile.getCompletedSteps().remove(OnboardingStep.DIFFICULTIES);
        }

        profile.setProgramCode(programCode);
        profile.markCompleted(OnboardingStep.PROGRAM);
        return save(profile);
    }

    public StudentProfile updateSemester(String userId, int semester) {
        StudentProfile profile = requireStep(userId, OnboardingStep.SEMESTER);

        if (profile.getProgramCode() == null) {
            throw ApiException.stepOutOfOrder("SEMESTER", "le parcours");
        }
        int semesterCount = content.programsFor(profile.getSystemCode()).stream()
                .filter(program -> program.code().equals(profile.getProgramCode()))
                .findFirst()
                .map(ContentClient.ProgramView::semesterCount)
                .orElseThrow(() -> ApiException.unknownProgram(profile.getProgramCode()));

        if (semester < 1 || semester > semesterCount) {
            throw ApiException.unknownSemester(semester, semesterCount);
        }

        if (!Objects.equals(profile.getSemester(), semester)) {
            profile.setCourseUnitCodes(new ArrayList<>());
            profile.getCompletedSteps().remove(OnboardingStep.COURSE_UNITS);
            profile.getCompletedSteps().remove(OnboardingStep.DIFFICULTIES);
        }

        profile.setSemester(semester);
        profile.markCompleted(OnboardingStep.SEMESTER);
        return save(profile);
    }

    public StudentProfile updateCourseUnits(String userId, List<String> unitCodes) {
        StudentProfile profile = requireStep(userId, OnboardingStep.COURSE_UNITS);

        if (profile.getProgramCode() == null) {
            throw ApiException.stepOutOfOrder("COURSE_UNITS", "le parcours");
        }

        Set<String> allowed = content
                .courseUnitsFor(profile.getSystemCode(), profile.getProgramCode(),
                        profile.getSemester())
                .stream()
                .map(ContentClient.CourseUnitView::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> validated = validateSelection(unitCodes, allowed, "unite d'enseignement");
        profile.setCourseUnitCodes(new ArrayList<>(validated));
        dropOrphanDifficulties(profile);

        profile.markCompleted(OnboardingStep.COURSE_UNITS);
        return save(profile);
    }

    public StudentProfile updateGoal(String userId, Goal goal, String targetExam, String note) {
        StudentProfile profile = getOrCreate(userId);
        profile.setGoal(goal);
        profile.setTargetExam(targetExam);
        profile.setGoalNote(note);
        profile.markCompleted(OnboardingStep.GOAL);
        return save(profile);
    }

    /** Une liste vide est une reponse valable : tout le monde n'est pas en difficulte. */
    public StudentProfile updateDifficulties(String userId, List<Difficulty> difficulties) {
        StudentProfile profile = getOrCreate(userId);

        if (profile.studiedCodes().isEmpty()) {
            throw ApiException.stepOutOfOrder("DIFFICULTIES", "ce que vous etudiez");
        }

        List<Difficulty> cleaned = difficulties == null ? List.of() : difficulties;
        cleaned.forEach(difficulty -> {
            if (!difficulty.isValid()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_difficulty",
                        "Chaque difficulte demande une matiere et une intensite de 1 a 3.");
            }
            // studiedCodes() couvre matieres, domaines et unites d'enseignement :
            // une difficulte se declare sur ce qu'on etudie, quel que soit le nom
            // que le cycle lui donne.
            if (!profile.studiedCodes().contains(difficulty.subjectCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "difficulty_on_unknown_subject",
                        "L'element " + difficulty.subjectCode() + " ne fait pas partie de ce que vous etudiez.");
            }
        });

        profile.setDifficulties(new ArrayList<>(cleaned));
        profile.markCompleted(OnboardingStep.DIFFICULTIES);
        return save(profile);
    }

    public StudentProfile updateAvailability(String userId, List<AvailabilitySlot> slots) {
        validateAvailability(slots);

        StudentProfile profile = getOrCreate(userId);
        profile.setAvailability(new ArrayList<>(slots));
        profile.markCompleted(OnboardingStep.AVAILABILITY);
        return save(profile);
    }

    // ------------------------------------------------------------------

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw ApiException.invalidBirthDate("La date de naissance est obligatoire.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw ApiException.invalidBirthDate("La date de naissance ne peut pas etre dans le futur.");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw ApiException.invalidBirthDate(
                    "L'age doit etre compris entre " + MIN_AGE + " et " + MAX_AGE + " ans.");
        }
    }

    private void validateAvailability(List<AvailabilitySlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw ApiException.invalidAvailability("Indiquez au moins un creneau de travail.");
        }
        slots.forEach(slot -> {
            if (!slot.isValid()) {
                throw ApiException.invalidAvailability(
                        "Chaque creneau demande un jour, une heure de debut et une heure de fin posterieure.");
            }
        });
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (slots.get(i).overlaps(slots.get(j))) {
                    throw ApiException.invalidAvailability(
                            "Deux creneaux se chevauchent le " + slots.get(i).day() + ".");
                }
            }
        }
    }

    /**
     * Charge le profil et verifie que cette etape concerne bien son cycle.
     *
     * <p>Sans ce controle, un enfant de maternelle pourrait se voir attribuer une
     * filiere de Terminale par un appel direct a l'API.
     */
    private StudentProfile requireStep(String userId, OnboardingStep step) {
        StudentProfile profile = getOrCreate(userId);

        if (profile.getSystemCode() == null || profile.getLevelCode() == null) {
            throw ApiException.stepOutOfOrder(step.name(), "le niveau scolaire");
        }
        if (!profile.requires(step)) {
            throw ApiException.stepNotApplicable(step.name(), profile.getLevelCode());
        }
        return profile;
    }

    /**
     * Traduit ce que content-service annonce en etapes locales.
     *
     * <p>Une etape inconnue est ignoree plutot que de faire echouer l'appel : un
     * content-service plus recent peut en annoncer que cette version ne sait pas
     * encore traiter, et cela ne doit pas bloquer une inscription.
     */
    private Set<OnboardingStep> toSteps(List<String> declared) {
        return declared.stream()
                .map(name -> {
                    try {
                        return OnboardingStep.valueOf(name);
                    } catch (IllegalArgumentException unknown) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(OnboardingStep::cycleDependent)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(OnboardingStep.class)));
    }

    /** Selection non vide, sans doublon, et dont chaque code est propose. */
    private List<String> validateSelection(List<String> chosen, Set<String> allowed, String label) {
        if (chosen == null || chosen.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "empty_selection",
                    "Choisissez au moins un element de type " + label + ".");
        }
        Set<String> unknown = new LinkedHashSet<>(chosen);
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw ApiException.unknownSubjects(unknown);
        }
        return List.copyOf(new LinkedHashSet<>(chosen));
    }

    /** Une difficulte sur quelque chose qu'on n'etudie plus n'a plus de sens. */
    private void dropOrphanDifficulties(StudentProfile profile) {
        List<String> studied = profile.studiedCodes();
        profile.getDifficulties().removeIf(
                difficulty -> !studied.contains(difficulty.subjectCode()));
    }

    private StudentProfile save(StudentProfile profile) {
        profile.setUpdatedAt(Instant.now());

        // Recalcule a chaque enregistrement : revenir modifier une etape peut aussi
        // bien achever le parcours que le rouvrir.
        if (onboarding.isComplete(profile)) {
            if (profile.getOnboardingCompletedAt() == null) {
                profile.setOnboardingCompletedAt(Instant.now());
            }
        } else {
            profile.setOnboardingCompletedAt(null);
        }

        return profiles.save(profile);
    }
}
