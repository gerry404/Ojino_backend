package com.schoolcopilot.support_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.support_service.repository.SupportRepositories.Faqs;
import com.schoolcopilot.support_service.web.AdminFaqController;
import com.schoolcopilot.support_service.web.FaqController;

/**
 * Verifie que le cablage tient : securite, configuration, beans, routes.
 *
 * <p>MongoDB est exclu parce que {@code mongoTemplate} ouvre sa connexion des sa
 * creation : sans cette exclusion, le test exigerait une base en marche et
 * echouerait partout ailleurs que sur ce poste.
 *
 * <p>Le repository, lui, doit donc etre fourni a la main : l'exclusion empeche
 * Spring Data de le fabriquer, alors que {@code FaqService} le reclame.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class SupportServiceApplicationTests {

    @MockitoBean
    Faqs faqs;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    FaqController faqController;

    @Autowired
    AdminFaqController adminFaqController;

    @Test
    void contextLoads() {
        assertThat(jwtDecoder).isNotNull();
        assertThat(faqController).isNotNull();
        assertThat(adminFaqController).isNotNull();
    }
}
