package com.schoolcopilot.media_service.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.media_service.domain.MediaAsset;
import com.schoolcopilot.media_service.service.MediaService;
import com.schoolcopilot.media_service.web.dto.MediaView;
import com.schoolcopilot.media_service.web.dto.UploadRequest;
import com.schoolcopilot.media_service.web.dto.UploadTicketView;

import jakarta.validation.Valid;

/**
 * Les fichiers de l'utilisateur connecte.
 *
 * <p>L'envoi se fait en deux appels : demander une adresse, puis confirmer une
 * fois le transfert termine. Le fichier lui-meme ne passe jamais par ce
 * controleur — c'est ce qui permet a un service Java de tenir des envois de
 * plusieurs mega sans saturer.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token : un identifiant
 * devine ne donne acces a rien, l'appartenance etant verifiee.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService media;

    public MediaController(MediaService media) {
        this.media = media;
    }

    /**
     * Etape 1 : demander une adresse d'envoi.
     *
     * <p>Type et taille sont verifies ici, avant tout transfert.
     */
    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadTicketView requestUpload(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UploadRequest request) {

        return UploadTicketView.from(media.requestUpload(jwt.getSubject(), request.purpose(),
                request.contentType(), request.sizeBytes(), request.filename()));
    }

    /** Etape 2 : confirmer, une fois le transfert termine. */
    @PostMapping("/uploads/{assetId}/confirm")
    public MediaView confirmUpload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String assetId) {

        MediaAsset asset = media.confirmUpload(jwt.getSubject(), assetId);
        return MediaView.withUrl(asset, media.downloadUrl(jwt.getSubject(), assetId));
    }

    @GetMapping("/{assetId}")
    public MediaView get(@AuthenticationPrincipal Jwt jwt, @PathVariable String assetId) {
        MediaAsset asset = media.get(jwt.getSubject(), assetId);
        return asset.isReady()
                ? MediaView.withUrl(asset, media.downloadUrl(jwt.getSubject(), assetId))
                : MediaView.from(asset);
    }

    @GetMapping
    public List<MediaView> list(@AuthenticationPrincipal Jwt jwt) {
        return media.listOwned(jwt.getSubject()).stream().map(MediaView::from).toList();
    }

    /** L'avatar courant. Renvoie 404 tant qu'aucun n'a ete depose. */
    @GetMapping("/avatar")
    public MediaView avatar(@AuthenticationPrincipal Jwt jwt) {
        MediaAsset asset = media.currentAvatar(jwt.getSubject())
                .orElseThrow(() -> com.schoolcopilot.media_service.exception.ApiException
                        .notFound("avatar"));
        return MediaView.withUrl(asset, media.downloadUrl(jwt.getSubject(), asset.id()));
    }

    @DeleteMapping("/{assetId}")
    public Map<String, String> delete(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String assetId) {
        media.delete(jwt.getSubject(), assetId);
        return Map.of("status", "deleted");
    }
}
