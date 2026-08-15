package com.schoolcopilot.notification_service.web.dto;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.domain.NotificationType;
import com.schoolcopilot.notification_service.domain.QuietHours;
import com.schoolcopilot.notification_service.exception.ApiException;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Les preferences de notification.
 *
 * @param zone fuseau de l'utilisateur. Sans lui, un eleve au Cameroun serait
 *        derange selon l'horloge du serveur.
 * @param dailyCap plafond de notifications intrusives par jour
 */
public record PreferencesRequest(

        @NotBlank(message = "Le fuseau horaire est obligatoire.")
        String zone,

        @Pattern(regexp = "fr|en", message = "Langue non prise en charge.")
        String language,

        boolean quietHoursEnabled,

        @JsonFormat(pattern = "HH:mm") LocalTime quietFrom,

        @JsonFormat(pattern = "HH:mm") LocalTime quietTo,

        Map<NotificationType, Set<NotificationChannel>> channelsByType,

        @Min(value = 1, message = "Le plafond doit valoir au moins 1.")
        @Max(value = 50, message = "Un plafond au-dela de 50 n'en est plus un.")
        int dailyCap) {

    public NotificationPreferences toDomain(String userId) {
        QuietHours quietHours = new QuietHours(
                quietFrom == null ? LocalTime.of(21, 0) : quietFrom,
                quietTo == null ? LocalTime.of(7, 0) : quietTo,
                quietHoursEnabled);

        return new NotificationPreferences(userId, parseZone(), language == null ? "fr" : language,
                quietHours,
                channelsByType == null ? new EnumMap<>(NotificationType.class) : channelsByType,
                dailyCap, null);
    }

    private ZoneId parseZone() {
        try {
            return ZoneId.of(zone);
        } catch (java.time.DateTimeException e) {
            throw ApiException.unknownZone(zone);
        }
    }
}
