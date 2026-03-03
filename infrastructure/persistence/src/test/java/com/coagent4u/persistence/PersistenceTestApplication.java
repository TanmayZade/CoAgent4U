package com.coagent4u.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot configuration for persistence integration tests.
 * Scans only the persistence package for JPA entities and repositories.
 */
@SpringBootApplication
@EntityScan("com.coagent4u.persistence")
@EnableJpaRepositories("com.coagent4u.persistence")
public class PersistenceTestApplication {
}
