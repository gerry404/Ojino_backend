package com.schoolcopilot.user_service.web.admin.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.user_service.domain.profile.Goal;
import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;

/**
 * Vue back-office d'un profil.
 *
 * <p>Distincte de celle rendue aux applications : elle montre l'avancement du
 * parcours d'inscription, utile pour comprendre ou les eleves abandonnent, et que
 * l'interesse n'a aucune raison de consulter sous cette forme.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminProfileView(
        String userId,
        String fullName,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
        Integer age,
        String avatarUrl,
        String systemCode,
        String levelCode,
        String trackCode,
        List<String> subjectCodes,
        int difficultyCount,
        Goal goal,
        String targetExam,
        long weeklyMinutes,
        List<OnboardingStep> completedSteps,
        boolean onboardingComplete,
        Instant onboardingCompletedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminProfileView from(StudentProfile profile) {
        return new AdminProfileView(
                profile.getId(),
                profile.fullName(),
                profile.getBirthDate(),
                profile.age(),
                profile.getAvatarUrl(),
                profile.getSystemCode(),
                profile.getLevelCode(),
                profile.getTrackCode(),
                profile.getSubjectCodes(),
                profile.getDifficulties().size(),
                profile.getGoal(),
                profile.getTargetExam(),
                profile.weeklyMinutes(),
                OnboardingStep.ordered().stream().filter(profile::hasCompleted).toList(),
                profile.isOnboardingComplete(),
                profile.getOnboardingCompletedAt(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
