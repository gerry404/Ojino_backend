package com.schoolcopilot.user_service.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ojino.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
