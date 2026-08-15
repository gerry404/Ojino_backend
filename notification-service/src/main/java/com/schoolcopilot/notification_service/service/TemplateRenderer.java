package com.schoolcopilot.notification_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.schoolcopilot.notification_service.domain.NotificationType;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Le texte des notifications, par langue.
 *
 * <p>Les libelles vivent dans un fichier et non dans le code : les corriger est
 * un travail de redaction, qui ne doit pas demander de recompiler. Ils seront
 * relus par quelqu'un d'autre que le developpeur.
 *
 * <p>Les substitutions sont volontairement rudimentaires — {@code {notion}} —
 * plutot qu'un moteur de gabarits complet. Une notification tient en deux lignes,
 * et un moteur capable d'executer du code sur des donnees venues d'ailleurs serait
 * une porte d'entree inutile.
 */
@Component
public class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);
    private static final String FILE = "templates/notifications.json";
    private static final String FALLBACK_LANGUAGE = "fr";

    private final Map<String, Map<String, Template>> byLanguage;

    public TemplateRenderer(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(FILE).getInputStream()) {
            this.byLanguage = objectMapper.readValue(input,
                    new TypeReference<Map<String, Map<String, Template>>>() {
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record Template(String title, String body) {
    }

    public record Rendered(String title, String body) {
    }

    /**
     * @param language langue demandee. Une langue inconnue retombe sur le
     *        francais plutot que d'echouer : mieux vaut une notification dans la
     *        mauvaise langue que pas de notification.
     */
    public Rendered render(NotificationType type, String language, Map<String, String> values) {
        Template template = lookup(type, language);
        if (template == null) {
            log.error("Aucun gabarit pour {} en {} : notification sans texte.", type, language);
            return new Rendered(type.name(), "");
        }
        return new Rendered(substitute(template.title(), values),
                substitute(template.body(), values));
    }

    private Template lookup(NotificationType type, String language) {
        Map<String, Template> templates = byLanguage.get(language);
        if (templates == null || !templates.containsKey(type.name())) {
            templates = byLanguage.get(FALLBACK_LANGUAGE);
        }
        return templates == null ? null : templates.get(type.name());
    }

    /**
     * Remplace les marqueurs presents dans les donnees.
     *
     * <p>Un marqueur sans valeur est laisse tel quel : cela se voit tout de suite
     * a la relecture, alors qu'un vide silencieux produit des phrases tronquees
     * que personne ne remarque avant la production.
     */
    private String substitute(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }
}
