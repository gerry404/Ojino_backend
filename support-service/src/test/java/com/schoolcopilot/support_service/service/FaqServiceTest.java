package com.schoolcopilot.support_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.LocalizedText;
import com.schoolcopilot.support_service.domain.PublicationStatus;
import com.schoolcopilot.support_service.exception.ApiException;
import com.schoolcopilot.support_service.repository.SupportRepositories.Faqs;
import com.schoolcopilot.support_service.web.dto.FaqEntryUpsertRequest;

/**
 * Les regles du centre d'aide, verifiees sans Spring ni base : Mockito seul,
 * donc quelques millisecondes.
 *
 * <p>Les noms de tests enoncent ce qui doit rester vrai. Le jour ou l'un casse,
 * son nom dit ce qu'on vient de perdre.
 */
@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    private static final String CODE = "CHANGER_CLASSE";

    @Mock
    Faqs faqs;

    @InjectMocks
    FaqService service;

    // -----------------------------------------------------------------------
    // Creation
    // -----------------------------------------------------------------------

    @Test
    void uneCreationEstToujoursUnBrouillon() {
        when(faqs.existsByCode(CODE)).thenReturn(false);
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        FaqEntry cree = service.create(requete("COMPTE", 10));

        assertThat(cree.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(cree.archived()).isFalse();
        assertThat(cree.isVisible()).isFalse();
    }

    @Test
    void uneCreationLaisseMongoGenererLIdentifiant() {
        when(faqs.existsByCode(CODE)).thenReturn(false);
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.create(requete("COMPTE", 10)).id()).isNull();
    }

    @Test
    void unCodeDejaPrisEstRefuseEnConflit() {
        when(faqs.existsByCode(CODE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(requete("COMPTE", 10)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "faq_code_already_exists");

        verify(faqs, never()).save(any(FaqEntry.class));
    }

    // -----------------------------------------------------------------------
    // Modification
    // -----------------------------------------------------------------------

    @Test
    void leCodeNEstPasModifiable() {
        when(faqs.findByCode(CODE)).thenReturn(
                Optional.of(entree(PublicationStatus.PUBLISHED, false)));
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        FaqEntry modifiee = service.update(CODE, new FaqEntryUpsertRequest(
                "UN_AUTRE_CODE",
                "FACTURATION",
                new LocalizedText("Nouvelle question", "New question"),
                new LocalizedText("Nouvelle reponse", "New answer"),
                42));

        assertThat(modifiee.code()).isEqualTo(CODE);
        assertThat(modifiee.category()).isEqualTo("FACTURATION");
        assertThat(modifiee.position()).isEqualTo(42);
    }

    @Test
    void modifierLeTexteNeRepubliePas() {
        when(faqs.findByCode(CODE)).thenReturn(
                Optional.of(entree(PublicationStatus.DRAFT, true)));
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        FaqEntry modifiee = service.update(CODE, requete("COMPTE", 1));

        assertThat(modifiee.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(modifiee.archived()).isTrue();
    }

    @Test
    void modifierUneEntreeInconnueRendUn404() {
        when(faqs.findByCode("INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("INCONNU", requete("COMPTE", 1)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "faq_entry_not_found");
    }

    // -----------------------------------------------------------------------
    // Cycle de vie
    // -----------------------------------------------------------------------

    @Test
    void publierRendLEntreeVisible() {
        when(faqs.findByCode(CODE)).thenReturn(
                Optional.of(entree(PublicationStatus.DRAFT, false)));
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.publish(CODE).isVisible()).isTrue();
    }

    @Test
    void archiverNeSupprimeRien() {
        when(faqs.findByCode(CODE)).thenReturn(
                Optional.of(entree(PublicationStatus.PUBLISHED, false)));
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        FaqEntry archivee = service.archive(CODE);

        assertThat(archivee.archived()).isTrue();
        assertThat(archivee.isVisible()).isFalse();
        // Le statut ne bouge pas : restaurer la remet en ligne telle quelle.
        assertThat(archivee.status()).isEqualTo(PublicationStatus.PUBLISHED);
        verify(faqs, never()).delete(any(FaqEntry.class));
    }

    @Test
    void restaurerRemetEnLigneUneEntreePubliee() {
        when(faqs.findByCode(CODE)).thenReturn(
                Optional.of(entree(PublicationStatus.PUBLISHED, true)));
        when(faqs.save(any(FaqEntry.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.restore(CODE).isVisible()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Lecture
    // -----------------------------------------------------------------------

    @Test
    void laListeClientNeDemandeQueLePublie() {
        when(faqs.findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED)).thenReturn(List.of());

        service.listVisible(null);

        verify(faqs).findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED);
        verify(faqs, never()).findAllByOrderByCategoryAscPositionAsc();
    }

    @Test
    void uneCategorieVideEquivautAAucunFiltre() {
        when(faqs.findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED)).thenReturn(List.of());

        service.listVisible("   ");

        verify(faqs).findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED);
    }

    @Test
    void lesCategoriesSontUniquesEtTriees() {
        when(faqs.findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED)).thenReturn(List.of(
                        entreeDeCategorie("FACTURATION"),
                        entreeDeCategorie("COMPTE"),
                        entreeDeCategorie("FACTURATION")));

        assertThat(service.listCategories()).containsExactly("COMPTE", "FACTURATION");
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private FaqEntryUpsertRequest requete(String categorie, int position) {
        return new FaqEntryUpsertRequest(
                CODE,
                categorie,
                new LocalizedText("Comment changer de classe ?", "How to change class?"),
                new LocalizedText("Depuis ton profil.", "From your profile."),
                position);
    }

    private FaqEntry entree(PublicationStatus statut, boolean archivee) {
        Instant creation = Instant.parse("2026-01-01T00:00:00Z");
        return new FaqEntry("64f0", CODE, "COMPTE",
                new LocalizedText("Question", "Question"),
                new LocalizedText("Reponse", "Answer"),
                10, statut, archivee, creation, creation);
    }

    private FaqEntry entreeDeCategorie(String categorie) {
        Instant creation = Instant.parse("2026-01-01T00:00:00Z");
        return new FaqEntry(null, CODE, categorie,
                new LocalizedText("Question", "Question"),
                new LocalizedText("Reponse", "Answer"),
                0, PublicationStatus.PUBLISHED, false, creation, creation);
    }
}
