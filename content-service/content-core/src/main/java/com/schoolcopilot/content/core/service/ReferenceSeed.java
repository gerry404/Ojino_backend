package com.schoolcopilot.content.core.service;

import java.util.List;

import com.schoolcopilot.content.core.domain.EducationCycle;

/** Reflet du fichier {@code reference/education-systems.json}. */
public record ReferenceSeed(List<SystemSeed> systems) {

    public record SystemSeed(
            String code,
            String country,
            String countryLabel,
            String label,
            String language,
            int displayOrder,
            List<LevelSeed> levels,
            List<TrackSeed> tracks,
            List<SubjectSeed> subjects) {
    }

    public record LevelSeed(
            String code,
            String label,
            EducationCycle cycle,
            int rank,
            int typicalAgeMin,
            int typicalAgeMax,
            boolean hasTracks) {
    }

    public record TrackSeed(
            String code,
            String label,
            String description,
            List<String> levelCodes,
            int displayOrder) {
    }

    public record SubjectSeed(
            String code,
            String label,
            List<String> levelCodes,
            List<String> trackCodes,
            boolean core,
            int displayOrder) {
    }
}
