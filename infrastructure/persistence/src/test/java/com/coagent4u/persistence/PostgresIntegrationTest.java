package com.coagent4u.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Testcontainers-based persistence integration tests.
 * Spins up a real PostgreSQL 16 container, runs Flyway V1–V7 migrations,
 * and configures Spring Data JPA with ddl-auto=validate.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Testcontainers
abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> PG;

    static {
        PG = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("coagent4u_test")
                .withUsername("test")
                .withPassword("test");
        PG.start();
    }

    @DynamicPropertySource
    static void pgProperties(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", () -> PG.getJdbcUrl() + "&stringtype=unspecified");
        reg.add("spring.datasource.username", PG::getUsername);
        reg.add("spring.datasource.password", PG::getPassword);
        reg.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        reg.add("spring.flyway.enabled", () -> "true");
        reg.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
}
