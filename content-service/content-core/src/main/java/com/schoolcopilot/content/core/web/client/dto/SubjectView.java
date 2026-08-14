package com.schoolcopilot.content.core.web.client.dto;

import com.schoolcopilot.content.core.domain.Subject;

/** @param core matiere du tronc commun, a cocher par defaut a l'inscription */
public record SubjectView(String code, String label, boolean core) {

    public static SubjectView from(Subject subject) {
        return new SubjectView(subject.code(), subject.label(), subject.core());
    }
}
