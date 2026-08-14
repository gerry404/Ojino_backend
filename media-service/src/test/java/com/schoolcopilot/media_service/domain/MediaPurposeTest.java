package com.schoolcopilot.media_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les regles par usage sont la premiere barriere : elles s'appliquent avant tout
 * transfert, et decident de ce qui entre dans le stockage.
 */
class MediaPurposeTest {

    @Test
    @DisplayName("un avatar n'accepte que des images")
    void avatarOnlyAcceptsImages() {
        assertThat(MediaPurpose.AVATAR.accepts("image/jpeg")).isTrue();
        assertThat(MediaPurpose.AVATAR.accepts("application/pdf")).isFalse();
    }

    @Test
    @DisplayName("un devoir accepte le PDF, car un eleve scanne plusieurs pages")
    void homeworkAcceptsPdf() {
        assertThat(MediaPurpose.HOMEWORK.accepts("application/pdf")).isTrue();
        assertThat(MediaPurpose.HOMEWORK.accepts("image/heic")).isTrue();
    }

    @Test
    @DisplayName("ce qui n'est pas explicitement permis est refuse")
    void theListIsAnAllowlist() {
        // Une liste noire laisserait passer tout ce a quoi personne n'a pense.
        assertThat(MediaPurpose.HOMEWORK.accepts("application/x-msdownload")).isFalse();
        assertThat(MediaPurpose.HOMEWORK.accepts("text/html")).isFalse();
        assertThat(MediaPurpose.HOMEWORK.accepts("application/zip")).isFalse();
    }

    @Test
    @DisplayName("le type est compare sans tenir compte de la casse")
    void contentTypeIsCaseInsensitive() {
        assertThat(MediaPurpose.AVATAR.accepts("IMAGE/JPEG")).isTrue();
    }

    @Test
    @DisplayName("un type absent est refuse")
    void missingContentTypeIsRejected() {
        assertThat(MediaPurpose.AVATAR.accepts(null)).isFalse();
    }

    @Test
    @DisplayName("un devoir a droit a plus de place qu'un avatar")
    void limitsDifferByPurpose() {
        // Une limite unique serait soit trop laxiste pour les avatars, soit trop
        // stricte pour les devoirs.
        assertThat(MediaPurpose.HOMEWORK.maxBytes())
                .isGreaterThan(MediaPurpose.AVATAR.maxBytes());
    }

    @Test
    @DisplayName("une taille nulle ou negative est refusee")
    void emptyFileIsRejected() {
        assertThat(MediaPurpose.AVATAR.acceptsSize(0)).isFalse();
        assertThat(MediaPurpose.AVATAR.acceptsSize(-1)).isFalse();
    }

    @Test
    @DisplayName("un fichier au-dela de la limite est refuse")
    void oversizedFileIsRejected() {
        assertThat(MediaPurpose.AVATAR.acceptsSize(MediaPurpose.AVATAR.maxBytes())).isTrue();
        assertThat(MediaPurpose.AVATAR.acceptsSize(MediaPurpose.AVATAR.maxBytes() + 1)).isFalse();
    }

    @Test
    @DisplayName("seul l'avatar est unique par utilisateur")
    void onlyAvatarIsSingleton() {
        // Un compte n'a qu'une photo de profil, mais autant de devoirs qu'il veut.
        assertThat(MediaPurpose.AVATAR.isSingleton()).isTrue();
        assertThat(MediaPurpose.HOMEWORK.isSingleton()).isFalse();
    }
}
