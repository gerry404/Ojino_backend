package com.schoolcopilot.media_service.web.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.media_service.domain.MediaAsset;
import com.schoolcopilot.media_service.domain.MediaPurpose;
import com.schoolcopilot.media_service.domain.MediaStatus;

/**
 * Un fichier tel que les applications le voient.
 *
 * <p>La cle de stockage n'y figure pas : c'est un detail d'implementation, et
 * elle changera le jour ou l'on passera a un stockage objet.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaView(
        String id,
        MediaPurpose purpose,
        String contentType,
        long sizeBytes,
        String filename,
        MediaStatus status,
        String url,
        Instant createdAt) {

    public static MediaView from(MediaAsset asset) {
        return build(asset, null);
    }

    /** Avec son adresse de lecture temporaire. */
    public static MediaView withUrl(MediaAsset asset, String url) {
        return build(asset, url);
    }

    private static MediaView build(MediaAsset asset, String url) {
        return new MediaView(asset.id(), asset.purpose(), asset.contentType(),
                asset.declaredBytes(), asset.originalFilename(), asset.status(), url,
                asset.createdAt());
    }
}
