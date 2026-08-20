package com.schoolcopilot.assistant_service.engine;

/**
 * Le moteur d'inference.
 *
 * <p>Quatrieme port du projet, apres {@code SmsSender}, {@code MediaStorage} et
 * {@code NotificationSender}. Le motif est le meme : une interface ici, une
 * implementation bouchonnee pour le developpement, une implementation reelle le
 * jour ou le service correspondant existe.
 *
 * <p>Ici l'implementation reelle sera {@code ai-service}, en FastAPI. Ce service
 * a ete ecrit <strong>avant</strong> lui, volontairement : quotas, garde-fous,
 * contexte et historique sont ainsi testables sans dependre d'un modele ni le
 * payer.
 */
public interface AiEngine {

    /**
     * @throws EngineException si l'inference echoue
     */
    AiReply complete(AiRequest request);

    /** Identifiant du moteur, pour la tracabilite. */
    String name();

    /**
     * Echec d'inference.
     *
     * @param retryable vrai si reessayer a une chance d'aboutir — surcharge
     *        passagere, delai depasse. Faux pour une requete refusee sur le fond,
     *        ou reessayer ne ferait que repayer le meme echec.
     */
    class EngineException extends RuntimeException {

        private final boolean retryable;

        public EngineException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
