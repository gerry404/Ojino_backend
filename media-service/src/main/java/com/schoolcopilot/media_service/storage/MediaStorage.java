package com.schoolcopilot.media_service.storage;

import java.time.Duration;

/**
 * Le stockage des octets.
 *
 * <p>Abstrait volontairement, comme {@code SmsSender} dans l'auth-service : le
 * jour ou un compte S3 est ouvert, il suffit de declarer un bean qui implemente
 * cette interface. Le reste du service ne sait pas — et n'a pas a savoir — ou les
 * fichiers atterrissent.
 *
 * <p>Le contrat impose des <strong>adresses signees</strong> : le client envoie
 * et recupere ses fichiers directement aupres du stockage. Faire transiter les
 * octets par la JVM la saturerait pour rien, et c'est precisement ce que S3 sait
 * faire mieux qu'un service applicatif.
 */
public interface MediaStorage {

    /**
     * Une adresse d'envoi temporaire.
     *
     * @param headers en-tetes que le client doit reprendre tels quels. S3 refuse
     *        l'envoi si le {@code Content-Type} signe ne correspond pas.
     */
    record UploadTarget(String url, String method, java.util.Map<String, String> headers) {
    }

    UploadTarget presignUpload(String storageKey, String contentType, long contentLength,
            Duration ttl);

    /** Une adresse de lecture temporaire. */
    String presignDownload(String storageKey, Duration ttl);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
