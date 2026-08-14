package com.schoolcopilot.user_service.web.client.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Une selection de codes du referentiel.
 *
 * <p>Partage par les etapes qui choisissent une liste — matieres, domaines
 * d'apprentissage, unites d'enseignement. Ce sont trois vocabulaires differents,
 * mais une seule et meme forme : un DTO par cycle n'aurait rien apporte.
 */
public record CodeListRequest(

        @NotEmpty(message = "Choisissez au moins un element.")
        List<String> codes) {
}
