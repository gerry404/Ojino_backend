package com.schoolcopilot.auth_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.auth_service.repository.OtpChallengeRepository;
import com.schoolcopilot.auth_service.repository.RefreshTokenRepository;
import com.schoolcopilot.auth_service.repository.UserRepository;
import com.schoolcopilot.auth_service.service.AuthService;
import com.schoolcopilot.auth_service.web.AuthController;

/**
 * Verifie que tout le contexte se cable : securite, encodage des JWT, providers
 * sociaux, controleurs.
 *
 * <p>Les autoconfigurations Mongo sont ecartees et les repositories remplaces par
 * des doublures : le bean {@code mongoTemplate} ouvre une connexion des sa
 * creation, ce test echouerait donc partout ou aucune base ne tourne. Le cablage
 * reel avec Mongo se verifie en lancant le service.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class AuthServiceApplicationTests {

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    OtpChallengeRepository otpChallengeRepository;

    @Autowired
    AuthService authService;

    @Autowired
    AuthController authController;

    @Test
    void contextLoads() {
        assertThat(authService).isNotNull();
        assertThat(authController).isNotNull();
    }
}
