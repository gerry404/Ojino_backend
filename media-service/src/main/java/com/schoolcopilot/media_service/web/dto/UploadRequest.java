package com.schoolcopilot.media_service.web.dto;

import com.schoolcopilot.media_service.domain.MediaPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Une demande d'envoi.
 *
 * <p>Le proprietaire ne figure pas ici : il vient du token. Personne ne peut donc
 * deposer un fichier au nom d'un autre.
 *
 * @param sizeBytes taille annoncee. Permet de refuser un fichier trop lourd avant
 *        le transfert, plutot qu'apres l'avoir recu.
 */
public record UploadRequest(

        @NotNull(message = "L'usage du fichier est obligatoire.")
        MediaPurpose purpose,

        @NotBlank(message = "Le type de fichier est obligatoire.")
        String contentType,

        @Positive(message = "La taille doit etre positive.")
        long sizeBytes,

        @Size(max = 255, message = "Le nom de fichier est trop long.")
        String filename) {
}
