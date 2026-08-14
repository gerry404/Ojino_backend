package com.schoolcopilot.content_service.domain.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un systeme scolaire : francophone camerounais, anglophone camerounais, francais...
 *
 * <p>Le referentiel vit en base et non dans le code : ouvrir l'application a un
 * nouveau pays revient a inserer des documents, pas a redeployer.
 *
 * @param code identifiant stable, par exemple {@code CM-FR}
 */
@Document(collection = "education_systems")
public record EducationSystem(
        @Id String code,
        String country,
        String countryLabel,
        String label,
        String language,
        int displayOrder,
        boolean active) {
}
