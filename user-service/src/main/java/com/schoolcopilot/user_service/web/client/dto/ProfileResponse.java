package com.schoolcopilot.user_service.web.client.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.user_service.domain.profile.AvailabilitySlot;
import com.schoolcopilot.user_service.domain.profile.Difficulty;
import com.schoolcopilot.user_service.domain.profile.Goal;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;

/**
 * Le profil tel que les applications le voient.
 *
 * <p>{@code age} est calcule a l'affichage a partir de la date de naissance, il
 * n'est jamais stocke.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        String id,
        String firstName,
        String lastName,
        String fullName,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
        Integer age,
        String avatarUrl,
        String systemCode,
        String levelCode,
        String trackCode,
        List<String> subjectCodes,
        Goal goal,
        String targetExam,
        String goalNote,
        List<DifficultyView> difficulties,
        List<SlotView> availability,
        long weeklyMinutes,
        boolean onboardingComplete,
        Instant createdAt,
        Instant updatedAt) {

    public record DifficultyView(String subjectCode, int severity, String note) {
    }

    public record SlotView(
            DayOfWeek day,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            long minutes) {
    }

    public static ProfileResponse from(StudentProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.fullName(),
                profile.getBirthDate(),
                profile.age(),
                profile.getAvatarUrl(),
                profile.getSystemCode(),
                profile.getLevelCode(),
                profile.getTrackCode(),
                profile.getSubjectCodes(),
                profile.getGoal(),
                profile.getTargetExam(),
                profile.getGoalNote(),
                profile.getDifficulties().stream().map(ProfileResponse::toView).toList(),
                profile.getAvailability().stream().map(ProfileResponse::toView).toList(),
                profile.weeklyMinutes(),
                profile.isOnboardingComplete(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private static DifficultyView toView(Difficulty difficulty) {
        return new DifficultyView(difficulty.subjectCode(), difficulty.severity(),
                difficulty.note());
    }

    private static SlotView toView(AvailabilitySlot slot) {
        return new SlotView(slot.day(), slot.startTime(), slot.endTime(), slot.minutes());
    }
}
