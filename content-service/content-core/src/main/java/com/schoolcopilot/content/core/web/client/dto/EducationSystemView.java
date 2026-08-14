package com.schoolcopilot.content.core.web.client.dto;

import com.schoolcopilot.content.core.domain.EducationSystem;

public record EducationSystemView(String code, String label, String country, String countryLabel,
        String language) {

    public static EducationSystemView from(EducationSystem system) {
        return new EducationSystemView(system.code(), system.label(), system.country(),
                system.countryLabel(), system.language());
    }
}
