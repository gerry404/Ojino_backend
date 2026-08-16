package com.schoolcopilot.assistant_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.schoolcopilot.assistant_service.client.ContentClient;
import com.schoolcopilot.assistant_service.client.LearningClient;
import com.schoolcopilot.assistant_service.client.ProfileClient;
import com.schoolcopilot.assistant_service.domain.StudyContext;

/**
 * Croise trois services pour savoir a qui l'on parle.
 *
 * <p>C'est ce qui distingue un assistant scolaire d'un agent generique : la
 * reponse s'appuie sur le niveau de l'eleve, sur ce qu'il maitrise deja et sur le
 * programme de sa classe.
 *
 * <p>Les trois pannes ne se traitent pas de la meme facon, et c'est deliberé :
 * <ul>
 *   <li>{@code user-service} — <strong>bloquant</strong>. Sans le niveau, pas de
 *       registre de langage, et repondre a un enfant de six ans comme a un lyceen
 *       est pire que ne pas repondre ;</li>
 *   <li>{@code learning-service} — degrade. La reponse sera moins ciblee ;</li>
 *   <li>{@code content-service} — degrade et signale. La reponse ne sera pas
 *       ancree dans le programme.</li>
 * </ul>
 */
@Component
public class ContextBuilder {

    /** Au-dela, on encombre la requete sans rien apporter. */
    private static final int MAX_GAPS = 5;

    private final ProfileClient profiles;
    private final LearningClient learning;
    private final ContentClient content;

    public ContextBuilder(ProfileClient profiles, LearningClient learning,
            ContentClient content) {
        this.profiles = profiles;
        this.learning = learning;
        this.content = content;
    }

    /**
     * @param notionCode notion du fil, si la conversation en cible une
     * @throws com.schoolcopilot.assistant_service.exception.ApiException si le
     *         profil est injoignable
     */
    public StudyContext build(String bearerToken, String notionCode) {
        ProfileClient.ProfileView profile = profiles.me(bearerToken);

        Optional<ContentClient.LevelView> level = profile.levelCode() == null
                ? Optional.empty()
                : content.level(profile.systemCode(), profile.levelCode());

        List<String> gaps = learning.gaps(profile.systemCode(), bearerToken).stream()
                .map(LearningClient.MasteryView::notionCode)
                .limit(MAX_GAPS)
                .toList();

        Optional<ContentClient.NotionView> notion = notionCode == null
                ? Optional.empty()
                : content.notion(profile.systemCode(), notionCode);

        List<String> prerequisites = notion
                .map(ContentClient.NotionView::prerequisiteCodes)
                .orElseGet(List::of);

        return new StudyContext(
                profile.systemCode(),
                profile.levelCode(),
                level.map(ContentClient.LevelView::label).orElse(null),
                level.map(ContentClient.LevelView::cycle).orElse(null),
                profile.trackCode(),
                profile.studied(),
                gaps,
                notion.map(ContentClient.NotionView::code).orElse(null),
                notion.map(ContentClient.NotionView::label).orElse(null),
                notion.map(ContentClient.NotionView::summary).orElse(null),
                prerequisites,
                // Le niveau est ce dont depend le registre : son absence est le
                // signe que l'ancrage a manque, meme si le reste a repondu.
                level.isPresent());
    }
}
