package com.schoolcopilot.content.core.web.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.content.core.domain.LearningResource;
import com.schoolcopilot.content.core.domain.ResourceType;

/** Un support d'apprentissage tel que les applications le voient. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceView(
        String code,
        ResourceType type,
        String label,
        String body,
        String url,
        int rank) {

    public static ResourceView from(LearningResource resource) {
        return new ResourceView(resource.code(), resource.type(), resource.label(),
                resource.body(), resource.url(), resource.rank());
    }
}
