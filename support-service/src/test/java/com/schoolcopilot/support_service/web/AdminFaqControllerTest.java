package com.schoolcopilot.support_service.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.schoolcopilot.support_service.config.CorsProperties;
import com.schoolcopilot.support_service.config.SecurityConfig;
import com.schoolcopilot.support_service.config.SecurityProperties;
import com.schoolcopilot.support_service.config.SupportProperties;
import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.LocalizedText;
import com.schoolcopilot.support_service.domain.PublicationStatus;
import com.schoolcopilot.support_service.exception.ApiException;
import com.schoolcopilot.support_service.service.FaqService;
import com.schoolcopilot.support_service.web.dto.FaqEntryUpsertRequest;

/**
 * Le contrat HTTP du back-office : ce qui entre, ce qui sort, et les erreurs.
 *
 * <p>Les clients testent le champ {@code code} de la reponse, jamais le message :
 * c'est pour ca qu'il est verifie ici et que le message ne l'est pas.
 */
@WebMvcTest(AdminFaqController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties({
        SecurityProperties.class, CorsProperties.class, SupportProperties.class})
class AdminFaqControllerTest {

    private static final String CORPS_VALIDE = """
            {
              "code": "CHANGER_CLASSE",
              "category": "COMPTE",
              "question": { "fr": "Comment changer de classe ?", "en": "How to change class?" },
              "answer": { "fr": "Depuis ton profil.", "en": "From your profile." },
              "position": 10
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqService faqService;

    @Test
    void uneCreationValideRessortEnBrouillon() throws Exception {
        when(faqService.create(any(FaqEntryUpsertRequest.class))).thenReturn(entree());

        mockMvc.perform(post("/api/v1/admin/faq").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void unCodeManquantRendUn400Detaille() throws Exception {
        String sansCode = """
                {
                  "category": "COMPTE",
                  "question": { "fr": "Sans code", "en": "No code" },
                  "answer": { "fr": "Sans code", "en": "No code" },
                  "position": 0
                }
                """;

        mockMvc.perform(post("/api/v1/admin/faq").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sansCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors.code").exists());

        verify(faqService, never()).create(any(FaqEntryUpsertRequest.class));
    }

    /**
     * Sans {@code @Valid} sur le champ imbrique, ce corps passerait : Spring
     * verifierait que {@code question} n'est pas nulle et s'arreterait la.
     */
    @Test
    void unTexteFrancaisVideEstRefuse() throws Exception {
        String francaisVide = """
                {
                  "code": "CHANGER_CLASSE",
                  "category": "COMPTE",
                  "question": { "fr": "   ", "en": "How to change class?" },
                  "answer": { "fr": "Depuis ton profil.", "en": "From your profile." },
                  "position": 10
                }
                """;

        mockMvc.perform(post("/api/v1/admin/faq").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(francaisVide))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors['question.fr']").exists());
    }

    @Test
    void unePositionNegativeEstRefusee() throws Exception {
        String positionNegative = CORPS_VALIDE.replace("\"position\": 10", "\"position\": -1");

        mockMvc.perform(post("/api/v1/admin/faq").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionNegative))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    /**
     * 409 et non 400 : la requete est valide, c'est l'etat du serveur qui la
     * rend impossible. La nuance decide si le client corrige sa saisie ou change
     * de code.
     */
    @Test
    void unCodeDejaPrisRessortEn409() throws Exception {
        when(faqService.create(any(FaqEntryUpsertRequest.class)))
                .thenThrow(ApiException.faqCodeAlreadyExists("CHANGER_CLASSE"));

        mockMvc.perform(post("/api/v1/admin/faq").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("faq_code_already_exists"));
    }

    private static FaqEntry entree() {
        Instant creation = Instant.parse("2026-01-01T00:00:00Z");
        return new FaqEntry("64f0", "CHANGER_CLASSE", "COMPTE",
                new LocalizedText("Comment changer de classe ?", "How to change class?"),
                new LocalizedText("Depuis ton profil.", "From your profile."),
                10, PublicationStatus.DRAFT, false, creation, creation);
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
