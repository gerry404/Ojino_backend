package com.schoolcopilot.media_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un fichier depose.
 *
 * <p>Le service stocke la <em>reference</em>, jamais le contenu : les octets
 * vivent dans le stockage objet, et le client les y envoie directement. Faire
 * transiter les fichiers par la JVM la saturerait pour rien.
 *
 * @param storageKey chemin dans le stockage. Genere par le service et jamais
 *        fourni par le client : sinon un chemin choisi permettrait d'ecrire par
 *        dessus le fichier de quelqu'un d'autre.
 * @param declaredBytes taille annoncee a la demande. Sert a refuser tout de suite
 *        ce qui depasse, avant meme le transfert.
 */
@Document(collection = "media_assets")
@CompoundIndex(name = "idx_media_owner_purpose", def = "{'ownerId': 1, 'purpose': 1, 'status': 1}")
public record MediaAsset(
        @Id String id,
        @Indexed String ownerId,
        MediaPurpose purpose,
        String storageKey,
        String contentType,
        long declaredBytes,
        String originalFilename,
        MediaStatus status,
        Instant createdAt,
        Instant confirmedAt,
        Instant deletedAt) {

    public boolean isReady() {
        return status == MediaStatus.READY;
    }

    public boolean belongsTo(String userId) {
        return ownerId.equals(userId);
    }

    /**
     * Une demande d'envoi jamais confirmee au-dela du delai.
     *
     * <p>Ce sont ces documents-la que le nettoyage recupere : un client qui perd
     * le reseau au milieu d'un envoi en laisse un derriere lui.
     */
    public boolean isStale(Instant threshold) {
        return status == MediaStatus.PENDING && createdAt.isBefore(threshold);
    }
}
