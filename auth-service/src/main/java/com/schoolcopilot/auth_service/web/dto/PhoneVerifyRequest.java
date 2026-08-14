package com.schoolcopilot.auth_service.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneVerifyRequest(

        @NotBlank(message = "L'identifiant du defi est obligatoire.")
        String challengeId,

        @NotBlank(message = "Le numero de telephone est obligatoire.")
        String phone,

        @NotBlank(message = "Le code est obligatoire.")
        String code) {
}
