package com.schoolcopilot.auth_service.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Format d'email invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 128, message = "Le mot de passe doit faire au moins 8 caracteres.")
        String password,

        @Size(max = 80, message = "Le nom affiche est trop long.")
        String displayName) {
}
