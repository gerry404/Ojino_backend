package com.schoolcopilot.content.earlyyears.web;

import com.schoolcopilot.content.earlyyears.domain.LearningDomain;

public record LearningDomainView(String code, String label, String description) {

    public static LearningDomainView from(LearningDomain domain) {
        return new LearningDomainView(domain.code(), domain.label(), domain.description());
    }
}
