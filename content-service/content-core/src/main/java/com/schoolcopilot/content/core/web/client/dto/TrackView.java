package com.schoolcopilot.content.core.web.client.dto;

import com.schoolcopilot.content.core.domain.Track;

public record TrackView(String code, String label, String description) {

    public static TrackView from(Track track) {
        return new TrackView(track.code(), track.label(), track.description());
    }
}
