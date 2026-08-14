package com.schoolcopilot.user_service.web.client.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Etape 1 : qui es-tu.
 *
 * <p>On demande la date de naissance et non l'age : un age saisi une fois devient
 * faux l'annee suivante, et le niveau scolaire suggere avec lui.
 */
public record IdentityRequest(

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 60, message = "Le prenom est trop long.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 60, message = "Le nom est trop long.")
        String lastName,

        @NotNull(message = "La date de naissance est obligatoire.")
        @Past(message = "La date de naissance doit etre dans le passe.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate) {
}
