package com.schoolcopilot.user_service.web.dto;

import com.schoolcopilot.user_service.domain.reference.Track;

public record TrackView(String code, String label, String description) {

    public static TrackView from(Track track) {
        return new TrackView(track.code(), track.label(), track.description());
    }
}
