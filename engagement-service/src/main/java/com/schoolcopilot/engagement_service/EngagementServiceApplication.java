package com.schoolcopilot.engagement_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @see com.schoolcopilot.engagement_service.service.StreakReminderJob qui previent
 *      des series a risque, active par {@code @EnableScheduling}
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EngagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EngagementServiceApplication.class, args);
	}

}
