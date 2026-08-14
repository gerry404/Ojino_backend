package com.schoolcopilot.content.app.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ojino.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
