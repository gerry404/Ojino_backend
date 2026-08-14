package com.schoolcopilot.media_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @see com.schoolcopilot.media_service.service.MediaCleanupJob pour le nettoyage
 *      periodique des envois abandonnes, active par {@code @EnableScheduling}
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MediaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaServiceApplication.class, args);
	}

}
