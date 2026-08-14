package com.schoolcopilot.user_service.domain.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Le profil scolaire d'un eleve.
 *
 * <p>L'identifiant est celui du compte, le {@code sub} de l'access token emis par
 * l'auth-service. Les deux services partagent donc une cle sans partager de base :
 * chacun reste maitre de ses donnees.
 *
 * <p>On enregistre la <strong>date de naissance</strong> et non l'age : un age
 * stocke devient faux au bout d'un an, et le niveau scolaire suggere avec lui.
 */
@Document(collection = "student_profiles")
public class StudentProfile {

    @Id
    private String id;

    private String firstName;

    private String lastName;

    private LocalDate birthDate;

    private String avatarUrl;

    private String systemCode;

    private String levelCode;

    /** Null tant que le niveau choisi ne se decline pas en filieres. */
    private String trackCode;

    private List<String> subjectCodes = new ArrayList<>();

    private Goal goal;

    /** Examen ou concours vise, quand l'objectif en suppose un. */
    private String targetExam;

    private String goalNote;

    private List<Difficulty> difficulties = new ArrayList<>();

    private List<AvailabilitySlot> availability = new ArrayList<>();

    private Set<OnboardingStep> completedSteps = EnumSet.noneOf(OnboardingStep.class);

    private Instant createdAt;

    private Instant updatedAt;

    /** Renseignee quand toutes les etapes obligatoires sont franchies. */
    private Instant onboardingCompletedAt;

    public static StudentProfile forUser(String userId) {
        StudentProfile profile = new StudentProfile();
        profile.id = userId;
        profile.createdAt = Instant.now();
        return profile;
    }

    /** Calcule a la volee depuis la date de naissance. */
    public Integer age() {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }

    public boolean isOnboardingComplete() {
        return onboardingCompletedAt != null;
    }

    public void markCompleted(OnboardingStep step) {
        completedSteps.add(step);
    }

    public boolean hasCompleted(OnboardingStep step) {
        return completedSteps.contains(step);
    }

    /** Le nom affichable, ou null si l'etape d'identite n'a pas encore ete faite. */
    public String fullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName))
                .trim();
    }

    /** Total des minutes de travail declarees sur une semaine. */
    public long weeklyMinutes() {
        return availability.stream().mapToLong(AvailabilitySlot::minutes).sum();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public String getTrackCode() {
        return trackCode;
    }

    public void setTrackCode(String trackCode) {
        this.trackCode = trackCode;
    }

    public List<String> getSubjectCodes() {
        return subjectCodes;
    }

    public void setSubjectCodes(List<String> subjectCodes) {
        this.subjectCodes = subjectCodes;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public String getTargetExam() {
        return targetExam;
    }

    public void setTargetExam(String targetExam) {
        this.targetExam = targetExam;
    }

    public String getGoalNote() {
        return goalNote;
    }

    public void setGoalNote(String goalNote) {
        this.goalNote = goalNote;
    }

    public List<Difficulty> getDifficulties() {
        return difficulties;
    }

    public void setDifficulties(List<Difficulty> difficulties) {
        this.difficulties = difficulties;
    }

    public List<AvailabilitySlot> getAvailability() {
        return availability;
    }

    public void setAvailability(List<AvailabilitySlot> availability) {
        this.availability = availability;
    }

    public Set<OnboardingStep> getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Set<OnboardingStep> completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public void setOnboardingCompletedAt(Instant onboardingCompletedAt) {
        this.onboardingCompletedAt = onboardingCompletedAt;
    }
}
