package com.schoolcopilot.engagement_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfig {

    /**
     * Les delais sont courts : une relance de motivation n'a aucune raison de
     * retenir un thread, et son echec est sans consequence.
     */
    @Bean
    RestClient notificationRestClient(DownstreamProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.notification().connectTimeout());
        factory.setReadTimeout(properties.notification().readTimeout());

        return RestClient.builder()
                .baseUrl(properties.notification().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
