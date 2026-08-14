package com.schoolcopilot.media_service.domain;

import java.util.Set;

/**
 * A quoi sert un fichier.
 *
 * <p>Ce n'est pas une simple etiquette : chaque usage a ses regles. Une photo de
 * profil est petite et forcement une image ; un devoir photographie peut etre un
 * PDF de plusieurs pages. Une limite unique pour tout le monde serait soit trop
 * laxiste pour les avatars, soit trop stricte pour les devoirs.
 *
 * @param maxBytes taille maximale acceptee
 * @param contentTypes types autorises. C'est une liste blanche, jamais une liste
 *        noire : ce qui n'est pas explicitement permis est refuse.
 */
public enum MediaPurpose {

    /** Photo de profil. Debloque l'etape PHOTO du parcours d'inscription. */
    AVATAR(2 * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),

    /**
     * Devoir ou exercice photographie, destine a la correction.
     *
     * <p>Le PDF est accepte : un eleve scanne souvent plusieurs pages d'un coup.
     */
    HOMEWORK(10 * 1024 * 1024,
            Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "application/pdf")),

    /** Illustration d'une ressource pedagogique, deposee par l'equipe. */
    RESOURCE_IMAGE(5 * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp", "image/svg+xml")),

    /** Piece jointe libre dans une conversation avec l'assistant. */
    ATTACHMENT(10 * 1024 * 1024,
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf"));

    private final long maxBytes;
    private final Set<String> contentTypes;

    MediaPurpose(long maxBytes, Set<String> contentTypes) {
        this.maxBytes = maxBytes;
        this.contentTypes = contentTypes;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public Set<String> contentTypes() {
        return contentTypes;
    }

    public boolean accepts(String contentType) {
        return contentType != null && contentTypes.contains(contentType.toLowerCase());
    }

    public boolean acceptsSize(long bytes) {
        return bytes > 0 && bytes <= maxBytes;
    }

    /**
     * Vrai si un seul fichier de cet usage peut exister par utilisateur.
     *
     * <p>Un compte n'a qu'un avatar : deposer le suivant remplace le precedent,
     * plutot que d'accumuler des photos que plus rien ne reference.
     */
    public boolean isSingleton() {
        return this == AVATAR;
    }
}
