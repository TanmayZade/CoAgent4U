package com.coagent4u;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CoAgent4U Application — AI-powered collaborative scheduling platform.
 *
 * <p>
 * This is the single entry point that assembles all Maven modules into
 * a Spring Boot application. Module wiring is handled by Spring's
 * component scanning across the {@code com.coagent4u} package hierarchy.
 * </p>
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.coagent4u.persistence")
@EntityScan(basePackages = "com.coagent4u.persistence")
public class CoAgent4UApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoAgent4UApplication.class, args);
    }
}
