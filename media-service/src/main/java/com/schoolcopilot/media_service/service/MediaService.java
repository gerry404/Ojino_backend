package com.schoolcopilot.media_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.media_service.config.MediaProperties;
import com.schoolcopilot.media_service.domain.MediaAsset;
import com.schoolcopilot.media_service.domain.MediaPurpose;
import com.schoolcopilot.media_service.domain.MediaStatus;
import com.schoolcopilot.media_service.exception.ApiException;
import com.schoolcopilot.media_service.repository.MediaAssetRepository;
import com.schoolcopilot.media_service.storage.MediaStorage;

/**
 * Le cycle de vie des fichiers.
 *
 * <p>L'envoi se fait en deux temps : {@link #requestUpload} delivre une adresse,
 * le client transfere directement vers le stockage, puis {@link #confirmUpload}
 * valide. Ce detour evite de faire transiter les octets par la JVM, mais il
 * suppose de gerer le cas ou la seconde etape n'arrive jamais — c'est le role de
 * {@link #cleanupAbandoned}.
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaAssetRepository assets;
    private final MediaStorage storage;
    private final MediaProperties properties;

    public MediaService(MediaAssetRepository assets, MediaStorage storage,
            MediaProperties properties) {
        this.assets = assets;
        this.storage = storage;
        this.properties = properties;
    }

    /** Une demande d'envoi acceptee : ou envoyer, et sous quelle reference. */
    public record UploadTicket(MediaAsset asset, MediaStorage.UploadTarget target) {
    }

    // ------------------------------------------------------------------

    /**
     * Verifie la demande et delivre une adresse d'envoi.
     *
     * <p>Le type et la taille sont controles <strong>avant</strong> le transfert :
     * refuser un fichier de dix mega apres l'avoir recu gaspille la bande passante
     * de l'eleve, souvent limitee.
     */
    public UploadTicket requestUpload(String ownerId, MediaPurpose purpose, String contentType,
            long declaredBytes, String originalFilename) {

        String normalized = contentType == null ? null : contentType.toLowerCase(Locale.ROOT);

        if (!purpose.accepts(normalized)) {
            throw ApiException.unsupportedType(contentType, purpose.contentTypes());
        }
        if (!purpose.acceptsSize(declaredBytes)) {
            throw ApiException.tooLarge(declaredBytes, purpose.maxBytes());
        }

        String storageKey = newStorageKey(ownerId, purpose, normalized);

        MediaAsset asset = assets.save(new MediaAsset(null, ownerId, purpose, storageKey,
                normalized, declaredBytes, originalFilename, MediaStatus.PENDING, Instant.now(),
                null, null));

        MediaStorage.UploadTarget target = storage.presignUpload(storageKey, normalized,
                declaredBytes, properties.uploadTtl());

        return new UploadTicket(asset, target);
    }

    /**
     * Confirme un transfert.
     *
     * <p>La presence du fichier est verifiee aupres du stockage : sans cela, un
     * client pourrait confirmer sans jamais avoir rien envoye, et la base
     * referencerait un fichier inexistant.
     */
    public MediaAsset confirmUpload(String ownerId, String assetId) {
        MediaAsset asset = requireOwned(ownerId, assetId);

        if (asset.status() == MediaStatus.READY) {
            throw ApiException.alreadyConfirmed();
        }
        if (!storage.exists(asset.storageKey())) {
            throw ApiException.notUploaded();
        }

        MediaAsset confirmed = assets.save(new MediaAsset(asset.id(), asset.ownerId(),
                asset.purpose(), asset.storageKey(), asset.contentType(), asset.declaredBytes(),
                asset.originalFilename(), MediaStatus.READY, asset.createdAt(), Instant.now(),
                null));

        // Un compte n'a qu'un avatar : le precedent devient inutile des que le
        // nouveau est confirme.
        if (asset.purpose().isSingleton()) {
            replacePrevious(confirmed);
        }

        return confirmed;
    }

    /** L'adresse de lecture d'un fichier confirme. */
    public String downloadUrl(String ownerId, String assetId) {
        MediaAsset asset = requireOwned(ownerId, assetId);
        if (!asset.isReady()) {
            throw ApiException.notReady();
        }
        return storage.presignDownload(asset.storageKey(), properties.downloadTtl());
    }

    public MediaAsset get(String ownerId, String assetId) {
        return requireOwned(ownerId, assetId);
    }

    public List<MediaAsset> listOwned(String ownerId) {
        return assets.findByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, MediaStatus.READY);
    }

    /** L'avatar courant, s'il existe. */
    public java.util.Optional<MediaAsset> currentAvatar(String ownerId) {
        return assets.findByOwnerIdAndPurposeAndStatus(ownerId, MediaPurpose.AVATAR,
                MediaStatus.READY).stream().findFirst();
    }

    public void delete(String ownerId, String assetId) {
        markDeleted(requireOwned(ownerId, assetId));
    }

    // ------------------------------------------------------------------
    // Nettoyage
    // ------------------------------------------------------------------

    /**
     * Efface les demandes d'envoi jamais confirmees.
     *
     * <p>Un client qui perd le reseau au milieu d'un transfert en laisse une
     * derriere lui. Sans ce passage, elles s'accumulent indefiniment, et avec elles
     * des fichiers partiels que plus rien ne reference.
     *
     * @return le nombre de demandes abandonnees nettoyees
     */
    public int cleanupAbandoned() {
        Instant threshold = Instant.now().minus(properties.pendingTtl());
        List<MediaAsset> abandoned =
                assets.findByStatusAndCreatedAtBefore(MediaStatus.PENDING, threshold);

        abandoned.forEach(this::markDeleted);

        if (!abandoned.isEmpty()) {
            log.info("{} demandes d'envoi abandonnees nettoyees.", abandoned.size());
        }
        return abandoned.size();
    }

    // ------------------------------------------------------------------

    /**
     * Efface le fichier puis le document, dans cet ordre.
     *
     * <p>Si l'effacement du stockage echoue, le document reste et la suppression
     * pourra etre reessayee. L'inverse laisserait un fichier orphelin sans aucune
     * trace pour le retrouver.
     */
    private void markDeleted(MediaAsset asset) {
        try {
            storage.delete(asset.storageKey());
        } catch (RuntimeException e) {
            log.error("Effacement de {} impossible, document conserve pour reessai : {}",
                    asset.storageKey(), e.getMessage());
            return;
        }

        assets.save(new MediaAsset(asset.id(), asset.ownerId(), asset.purpose(),
                asset.storageKey(), asset.contentType(), asset.declaredBytes(),
                asset.originalFilename(), MediaStatus.DELETED, asset.createdAt(),
                asset.confirmedAt(), Instant.now()));
    }

    private void replacePrevious(MediaAsset current) {
        assets.findByOwnerIdAndPurposeAndStatus(current.ownerId(), current.purpose(),
                MediaStatus.READY).stream()
                .filter(asset -> !asset.id().equals(current.id()))
                .forEach(this::markDeleted);
    }

    private MediaAsset requireOwned(String ownerId, String assetId) {
        MediaAsset asset = assets.findById(assetId)
                .orElseThrow(() -> ApiException.notFound(assetId));
        if (!asset.belongsTo(ownerId)) {
            throw ApiException.notYours();
        }
        return asset;
    }

    /**
     * Genere une cle de stockage.
     *
     * <p>Toujours cote serveur, jamais fournie par le client : une cle choisie
     * permettrait d'ecrire par dessus le fichier de quelqu'un d'autre. Le prefixe
     * par proprietaire et par usage rend en prime le stockage lisible a l'oeil nu.
     */
    private String newStorageKey(String ownerId, MediaPurpose purpose, String contentType) {
        return purpose.name().toLowerCase(Locale.ROOT) + "/" + ownerId + "/"
                + UUID.randomUUID() + extensionOf(contentType);
    }

    private String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            case "image/svg+xml" -> ".svg";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
