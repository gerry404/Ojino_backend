package com.schoolcopilot.media_service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schoolcopilot.media_service.config.MediaProperties;

/**
 * Stockage sur disque, pour le developpement.
 *
 * <p>Reproduit fidelement le contrat de S3 — adresses signees, envoi direct par le
 * client — mais les adresses pointent vers ce service au lieu d'un stockage objet.
 * Cela permet d'eprouver tout le parcours sans compte cloud, et surtout de le
 * remplacer plus tard sans qu'aucun appelant ne s'en apercoive.
 *
 * <p>Elle s'efface d'elle-meme des qu'un autre {@link MediaStorage} est declare
 * (voir {@code StorageConfig}).
 */
public class LocalMediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorage.class);

    private final Path root;
    private final MediaProperties properties;
    private final SignedLinks signedLinks;

    public LocalMediaStorage(MediaProperties properties, SignedLinks signedLinks) {
        this.properties = properties;
        this.signedLinks = signedLinks;
        this.root = Path.of(properties.local().directory()).toAbsolutePath().normalize();
        log.warn("[STOCKAGE LOCAL] Les fichiers sont ecrits dans {}. "
                + "A remplacer par un stockage objet en production.", root);
    }

    @Override
    public UploadTarget presignUpload(String storageKey, String contentType, long contentLength,
            Duration ttl) {
        return new UploadTarget(link(storageKey, "PUT", ttl), "PUT",
                Map.of("Content-Type", contentType));
    }

    @Override
    public String presignDownload(String storageKey, Duration ttl) {
        return link(storageKey, "GET", ttl);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolve(storageKey));
    }

    // ------------------------------------------------------------------
    // Utilise par le controleur de transfert, propre au stockage local
    // ------------------------------------------------------------------

    /** @return la taille reellement ecrite, a confronter a celle annoncee */
    public long write(String storageKey, InputStream content) {
        try {
            Path target = resolve(storageKey);
            Files.createDirectories(target.getParent());
            return Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path read(String storageKey) {
        return resolve(storageKey);
    }

    public boolean isValid(String storageKey, String operation, long expiresAt, String signature) {
        return signedLinks.isValid(storageKey, operation, expiresAt, signature);
    }

    // ------------------------------------------------------------------

    private String link(String storageKey, String operation, Duration ttl) {
        long expiresAt = signedLinks.expiryFor(ttl);
        String signature = signedLinks.sign(storageKey, operation, expiresAt);

        return properties.publicBaseUrl()
                + "/api/v1/media/blob/" + encode(storageKey)
                + "?expiresAt=" + expiresAt
                + "&signature=" + encode(signature);
    }

    /**
     * Resout une cle sous la racine, en refusant d'en sortir.
     *
     * <p>Sans cette verification, une cle contenant {@code ../} permettrait de lire
     * ou d'ecrire n'importe ou sur le disque du serveur. Les cles sont generees par
     * le service, mais cette barriere ne doit pas dependre de cette hypothese.
     */
    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Cle de stockage hors de la racine : " + storageKey);
        }
        return resolved;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
