package com.schoolcopilot.notification_service.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.notification_service.service.NotificationService;
import com.schoolcopilot.notification_service.web.dto.NotificationView;
import com.schoolcopilot.notification_service.web.dto.SendRequest;

import jakarta.validation.Valid;

/**
 * Le point d'entree des autres services.
 *
 * <p>Protege par un secret partage plutot que par un jeton d'utilisateur : les
 * appelants declenchent des notifications depuis des taches planifiees, ou aucun
 * utilisateur n'est connecte.
 *
 * <p>La reponse liste ce qui a ete mis en file. Une liste vide n'est pas une
 * erreur : l'utilisateur peut avoir coupe ce type, ou avoir atteint son plafond,
 * ou la notification peut etre un doublon. L'appelant n'a pas a s'en soucier —
 * c'est precisement le role de ce service.
 */
@RestController
@RequestMapping("/api/v1/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notifications;

    public InternalNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<NotificationView> send(@Valid @RequestBody SendRequest request) {
        return notifications.enqueue(request.userId(), request.type(), request.values(),
                request.dedupeKey()).stream()
                .map(NotificationView::from)
                .toList();
    }
}
