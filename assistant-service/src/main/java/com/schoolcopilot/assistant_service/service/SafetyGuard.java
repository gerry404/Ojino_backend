package com.schoolcopilot.assistant_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.schoolcopilot.assistant_service.domain.LanguageRegister;

import tools.jackson.databind.ObjectMapper;

/**
 * Les garde-fous, en entree et en sortie.
 *
 * <p><strong>Ce filtre est une premiere barriere, pas une garantie.</strong> Une
 * detection par expressions attrape le grossier et rate le subtil. La vraie
 * moderation viendra avec {@code ai-service} et un modele dedie. Il faut le dire
 * ici, sans quoi quelqu'un finira par lui faire confiance pour ce qu'il ne sait
 * pas faire.
 *
 * <p>Un cas est traite a part : la detresse. Le public commence a la maternelle,
 * et un enfant qui ecrit qu'il veut mourir ne doit recevoir ni une reponse
 * generee, ni un refus sec. Il recoit une orientation, ecrite a l'avance et
 * relue.
 */
@Component
public class SafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuard.class);
    private static final String RULES_FILE = "safety/rules.json";
    private static final String DISTRESS_CODE = "sante";
    private static final String HOMEWORK_CODE = "devoir_a_la_place";

    private final Rules rules;

    public SafetyGuard(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(RULES_FILE).getInputStream()) {
            this.rules = objectMapper.readValue(input, Rules.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record Rules(List<Rule> refus, java.util.Map<String, String> orientationDetresse) {
    }

    record Rule(String code, String libelle, String consigne, List<String> declencheurs) {

        // Un accesseur redefini dans un record doit rester public, meme quand le
        // record lui-meme ne l'est pas.
        public List<String> declencheurs() {
            return declencheurs == null ? List.of() : declencheurs;
        }
    }

    /** Ce que la barriere decide d'une question. */
    public sealed interface Decision {

        /** La question part au moteur. */
        record Allow() implements Decision {
        }

        /**
         * On repond sans appeler le moteur.
         *
         * <p>Le cas de la detresse : la reponse est ecrite a l'avance et relue.
         * Laisser un modele improviser la-dessus serait irresponsable.
         */
        record Intercept(String code, String reply) implements Decision {
        }

        /**
         * La question part, mais avec une consigne supplementaire.
         *
         * <p>Le cas du devoir : on ne refuse pas d'aider, on refuse de faire a la
         * place. Refuser tout court pousserait l'eleve vers un autre outil qui,
         * lui, ecrira le devoir.
         */
        record AllowWithGuidance(String code, String guidance) implements Decision {
        }
    }

    /** Examine une question avant tout appel au moteur. */
    public Decision inspect(String question, String language) {
        String normalized = normalize(question);

        for (Rule rule : rules.refus()) {
            if (matches(normalized, rule)) {
                if (DISTRESS_CODE.equals(rule.code())) {
                    log.info("Question interceptee : orientation vers un adulte.");
                    return new Decision.Intercept(rule.code(), distressReply(language));
                }
                if (HOMEWORK_CODE.equals(rule.code())) {
                    return new Decision.AllowWithGuidance(rule.code(), rule.consigne());
                }
                return new Decision.AllowWithGuidance(rule.code(), rule.consigne());
            }
        }

        return new Decision.Allow();
    }

    /**
     * Les consignes generales transmises au moteur a chaque appel.
     *
     * <p>Toutes les regles, et pas seulement celles declenchees : ce que
     * l'assistant ne fait pas ne depend pas de la formulation de la question.
     */
    public List<String> standingGuidance() {
        return rules.refus().stream().map(Rule::consigne).toList();
    }

    /**
     * Verifie une reponse avant de la rendre.
     *
     * <p>Un modele peut produire n'importe quoi, et le public a six ans. Le
     * controle porte ici sur la longueur : une reponse de deux mille caracteres a
     * un enfant de grande section est un echec meme si chaque mot est juste, parce
     * qu'il ne la lira pas.
     */
    public String enforceReplyLength(String reply, LanguageRegister register) {
        if (reply.length() <= register.maxReplyChars()) {
            return reply;
        }

        log.warn("Reponse de {} caracteres tronquee a {} pour le registre {}.",
                reply.length(), register.maxReplyChars(), register);

        // Coupe a la derniere phrase complete plutot qu'au milieu d'un mot : une
        // reponse tronquee net est plus deroutante qu'une reponse plus courte.
        String truncated = reply.substring(0, register.maxReplyChars());
        int lastStop = Math.max(truncated.lastIndexOf('.'), truncated.lastIndexOf('!'));

        return lastStop > register.maxReplyChars() / 2
                ? truncated.substring(0, lastStop + 1)
                : truncated;
    }

    // ------------------------------------------------------------------

    private boolean matches(String normalizedQuestion, Rule rule) {
        for (String trigger : rule.declencheurs()) {
            if (normalizedQuestion.contains(normalize(trigger))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Minuscules et accents retires.
     *
     * <p>Sans cela, « Suicide » ou « suicidé » passeraient a cote d'un declencheur
     * ecrit « suicide ». C'est le minimum, et ce n'est pas suffisant — voir la
     * mise en garde en tete de classe.
     */
    private String normalize(String text) {
        String lowered = text.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String distressReply(String language) {
        return rules.orientationDetresse()
                .getOrDefault(language, rules.orientationDetresse().get("fr"));
    }
}
