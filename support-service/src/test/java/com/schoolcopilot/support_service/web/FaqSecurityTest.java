package com.schoolcopilot.support_service.web;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.schoolcopilot.support_service.config.CorsProperties;
import com.schoolcopilot.support_service.config.SecurityConfig;
import com.schoolcopilot.support_service.config.SecurityProperties;
import com.schoolcopilot.support_service.config.SupportProperties;
import com.schoolcopilot.support_service.service.FaqService;

/**
 * La frontiere du back-office, verifiee pour de vrai.
 *
 * <p>C'est le test qui compte le plus de ce service : une regle de securite se
 * lit tres bien et se casse tres discretement. Ici on n'inspecte pas la
 * configuration, on envoie des requetes et on regarde le statut qui revient.
 *
 * <p>{@code jwt()} fabrique une authentification deja validee : le decodeur
 * n'est pas sollicite, on teste les regles d'acces et non la cryptographie.
 */
@WebMvcTest({FaqController.class, AdminFaqController.class})
@Import(SecurityConfig.class)
@EnableConfigurationProperties({
        SecurityProperties.class, CorsProperties.class, SupportProperties.class})
class FaqSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqService faqService;

    @Test
    void sansJetonLeBackOfficeRepond401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/faq"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 401 et 403 ne sont pas interchangeables : « je ne sais pas qui tu es » et
     * « je sais qui tu es, et tu n'as pas le droit ».
     */
    @Test
    void unJetonEleveNOuvrePasLeBackOffice() throws Exception {
        mockMvc.perform(get("/api/v1/admin/faq").with(eleve()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unJetonAdminOuvreLeBackOffice() throws Exception {
        when(faqService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/faq").with(admin()))
                .andExpect(status().isOk());
    }

    /**
     * {@code ojino.support.faq.public-access} vaut false : la FAQ est fermee, et
     * la decision tient dans une ligne de configuration, pas dans le code.
     */
    @Test
    void laFaqFermeeExigeUnJeton() throws Exception {
        mockMvc.perform(get("/api/v1/faq"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laFaqEstLisibleParUnEleveConnecte() throws Exception {
        when(faqService.listVisible(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/faq").with(eleve()))
                .andExpect(status().isOk());
    }

    /** Un eleve n'ecrit pas, meme sur le chemin qu'il a le droit de lire. */
    @Test
    void unEleveNePeutPasEcrireDansLaFaq() throws Exception {
        mockMvc.perform(get("/api/v1/admin/faq/CHANGER_CLASSE").with(eleve()))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor eleve() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
