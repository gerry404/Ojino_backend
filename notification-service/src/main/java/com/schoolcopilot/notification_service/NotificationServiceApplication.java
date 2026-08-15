package com.schoolcopilot.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @see com.schoolcopilot.notification_service.service.NotificationDispatchJob qui
 *      vide la file, active par {@code @EnableScheduling}
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
