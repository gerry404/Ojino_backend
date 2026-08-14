package com.schoolcopilot.content.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * L'application, qui assemble le coeur et les modules de cycle embarques.
 *
 * <p>Les trois annotations pointent volontairement sur {@code com.schoolcopilot.content}
 * et non sur le paquet de cette classe : sans cela, Spring ne verrait que
 * {@code content-app} et ignorerait le coeur comme tous les cycles.
 *
 * <p>C'est la contrepartie du decoupage en modules, et le seul endroit ou elle se
 * paie. Ajouter un cycle reste une ligne dans {@code pom.xml}.
 */
@SpringBootApplication(scanBasePackages = "com.schoolcopilot.content")
@ConfigurationPropertiesScan("com.schoolcopilot.content")
@EnableMongoRepositories("com.schoolcopilot.content")
public class ContentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentServiceApplication.class, args);
	}

}
