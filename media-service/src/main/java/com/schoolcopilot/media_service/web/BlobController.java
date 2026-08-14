package com.schoolcopilot.media_service.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.media_service.exception.ApiException;
import com.schoolcopilot.media_service.storage.LocalMediaStorage;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Le transfert des octets, pour le stockage local uniquement.
 *
 * <p>Ce controleur tient le role du stockage objet : il recoit et rend les
 * fichiers aux adresses signees delivrees par {@link LocalMediaStorage}. Avec un
 * vrai S3 il disparait, et rien d'autre ne change — il n'existe donc que si le
 * stockage local est le stockage actif.
 *
 * <p>Ces routes sont <strong>publiques</strong>, et c'est normal : une adresse
 * pre-signee se consomme sans jeton, c'est tout son interet. La protection vient
 * de la signature et de son expiration, verifiees a chaque appel.
 */
@RestController
@RequestMapping("/api/v1/media/blob")
@ConditionalOnProperty(name = "ojino.media.storage", havingValue = "local",
        matchIfMissing = true)
public class BlobController {

    private final LocalMediaStorage storage;

    public BlobController(LocalMediaStorage storage) {
        this.storage = storage;
    }

    @PutMapping("/{storageKey}")
    public ResponseEntity<Void> upload(@PathVariable String storageKey,
            @RequestParam long expiresAt, @RequestParam String signature,
            HttpServletRequest request) throws IOException {

        requireValid(storageKey, "PUT", expiresAt, signature);

        try (InputStream body = request.getInputStream()) {
            storage.write(storageKey, body);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{storageKey}")
    public ResponseEntity<Resource> download(@PathVariable String storageKey,
            @RequestParam long expiresAt, @RequestParam String signature) throws IOException {

        requireValid(storageKey, "GET", expiresAt, signature);

        Path file = storage.read(storageKey);
        if (!Files.isRegularFile(file)) {
            throw ApiException.notFound(storageKey);
        }

        String contentType = Files.probeContentType(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,
                        contentType == null ? "application/octet-stream" : contentType)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new FileSystemResource(file));
    }

    private void requireValid(String storageKey, String operation, long expiresAt,
            String signature) {
        if (!storage.isValid(storageKey, operation, expiresAt, signature)) {
            throw ApiException.invalidLink();
        }
    }
}
