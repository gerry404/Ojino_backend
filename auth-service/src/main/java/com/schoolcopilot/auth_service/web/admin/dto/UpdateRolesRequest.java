package com.schoolcopilot.auth_service.web.admin.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

/** Remplace la liste des roles d'un compte. */
public record UpdateRolesRequest(

        @NotEmpty(message = "Un compte doit conserver au moins un role.")
        Set<String> roles) {
}
