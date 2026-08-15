package com.schoolcopilot.notification_service.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.service.NotificationService;
import com.schoolcopilot.notification_service.web.dto.NotificationView;
import com.schoolcopilot.notification_service.web.dto.PreferencesRequest;

import jakarta.validation.Valid;

/**
 * Les notifications de l'utilisateur connecte.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token : personne ne peut
 * lire la boite d'un autre ni modifier ses preferences.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    /** Les messages non lus dans l'application. */
    @GetMapping("/inbox")
    public List<NotificationView> inbox(@AuthenticationPrincipal Jwt jwt) {
        return notifications.inbox(jwt.getSubject()).stream()
                .map(NotificationView::from)
                .toList();
    }

    /** Pour la pastille de l'application, sans charger toute la liste. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", notifications.unreadCount(jwt.getSubject()));
    }

    /** L'historique, tous canaux confondus. */
    @GetMapping("/history")
    public List<NotificationView> history(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {
        return notifications.history(jwt.getSubject(), limit).stream()
                .map(NotificationView::from)
                .toList();
    }

    @PostMapping("/{id}/read")
    public NotificationView markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return NotificationView.from(notifications.markRead(jwt.getSubject(), id));
    }

    @GetMapping("/preferences")
    public NotificationPreferences preferences(@AuthenticationPrincipal Jwt jwt) {
        return notifications.preferencesOf(jwt.getSubject());
    }

    @PutMapping("/preferences")
    public NotificationPreferences updatePreferences(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PreferencesRequest request) {
        return notifications.updatePreferences(jwt.getSubject(),
                request.toDomain(jwt.getSubject()));
    }
}
