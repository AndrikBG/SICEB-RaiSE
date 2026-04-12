package com.siceb.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real PostgreSQL with Flyway migrations.
 * Extend this class to get a running PG 16 container with all migrations applied.
 *
 * Usage:
 * <pre>
 * class MyIntegrationTest extends IntegrationTestBase {
 *     @Autowired
 *     private JdbcTemplate jdbc;
 *
 *     @Test
 *     void myTest() { ... }
 * }
 * </pre>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("siceb_test")
            .withUsername("siceb_test")
            .withPassword("siceb_test")
            .withCommand("postgres", "-c", "log_statement=all");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
    }
}
