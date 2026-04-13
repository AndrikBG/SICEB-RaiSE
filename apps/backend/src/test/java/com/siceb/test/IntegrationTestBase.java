package com.siceb.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real PostgreSQL with Flyway migrations.
 * Uses the singleton container pattern — one PG container shared across all test classes
 * in the JVM, avoiding connection pool staleness when Spring context is cached.
 *
 * Usage:
 * <pre>
 * class MyIntegrationTest extends IntegrationTestBase {
 *     {@literal @}Autowired
 *     private JdbcTemplate jdbc;
 *
 *     {@literal @}Test
 *     void myTest() { ... }
 * }
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    protected static final PostgreSQLContainer<?> PG;

    static {
        PG = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("siceb_test")
                .withUsername("siceb_test")
                .withPassword("siceb_test")
                .withCommand("postgres", "-c", "log_statement=all");
        PG.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
    }
}
