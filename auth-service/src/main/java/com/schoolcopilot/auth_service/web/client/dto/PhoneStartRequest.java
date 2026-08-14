package com.schoolcopilot.auth_service.web.client.dto;

import jakarta.validation.constraints.NotBlank;

/** Demande d'envoi d'un code SMS. Le numero est attendu au format E.164. */
public record PhoneStartRequest(

        @NotBlank(message = "Le numero de telephone est obligatoire.")
        String phone) {
}
