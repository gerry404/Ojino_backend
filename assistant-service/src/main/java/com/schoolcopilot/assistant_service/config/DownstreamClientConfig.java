package com.schoolcopilot.assistant_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfig {

    @Bean
    RestClient profileRestClient(DownstreamProperties properties) {
        return build(properties.user());
    }

    @Bean
    RestClient learningRestClient(DownstreamProperties properties) {
        return build(properties.learning());
    }

    @Bean
    RestClient contentRestClient(DownstreamProperties properties) {
        return build(properties.content());
    }

    /**
     * Les delais d'attente ne sont pas optionnels : sans eux, un service bloque
     * immobiliserait un thread par question posee, jusqu'a saturation.
     */
    private RestClient build(DownstreamProperties.Service service) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(service.connectTimeout());
        factory.setReadTimeout(service.readTimeout());

        return RestClient.builder()
                .baseUrl(service.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
