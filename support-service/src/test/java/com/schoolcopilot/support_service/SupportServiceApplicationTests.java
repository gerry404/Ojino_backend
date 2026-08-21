package com.schoolcopilot.support_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Verifie que le cablage tient : securite, configuration, beans.
 *
 * <p>MongoDB est exclu parce que {@code mongoTemplate} ouvre sa connexion des sa
 * creation : sans cette exclusion, le test exigerait une base en marche et
 * echouerait partout ailleurs que sur ton poste.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class SupportServiceApplicationTests {

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        assertThat(jwtDecoder).isNotNull();
    }
}
