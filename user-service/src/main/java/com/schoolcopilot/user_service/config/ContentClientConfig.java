package com.schoolcopilot.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ContentClientConfig {

    /**
     * Les delais d'attente ne sont pas optionnels : sans eux, un content-service
     * bloque immobiliserait un thread de user-service par requete, jusqu'a
     * saturation.
     */
    @Bean
    RestClient contentRestClient(ContentProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
