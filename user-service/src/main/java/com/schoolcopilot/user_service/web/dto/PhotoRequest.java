package com.schoolcopilot.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Etape 2 : la photo de profil.
 *
 * <p>On stocke une URL, pas le fichier : l'envoi de l'image passera par un service
 * de stockage dedie, qui rendra cette URL.
 */
public record PhotoRequest(

        @NotBlank(message = "L'adresse de la photo est obligatoire.")
        String avatarUrl) {
}
