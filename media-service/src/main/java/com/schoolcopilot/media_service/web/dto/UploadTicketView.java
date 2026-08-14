package com.schoolcopilot.media_service.web.dto;

import java.util.Map;

import com.schoolcopilot.media_service.service.MediaService;

/**
 * Ou envoyer le fichier, et sous quelle reference le confirmer ensuite.
 *
 * <p>{@code headers} doit etre repris tel quel par le client : un stockage objet
 * refuse le transfert si le {@code Content-Type} ne correspond pas a celui qui a
 * ete signe.
 *
 * <p>La cle de stockage n'est pas exposee : le client n'en a pas besoin, et la
 * connaitre l'inviterait a construire des adresses lui-meme.
 */
public record UploadTicketView(
        String assetId,
        String uploadUrl,
        String method,
        Map<String, String> headers) {

    public static UploadTicketView from(MediaService.UploadTicket ticket) {
        return new UploadTicketView(ticket.asset().id(), ticket.target().url(),
                ticket.target().method(), ticket.target().headers());
    }
}
