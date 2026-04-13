package com.siceb.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test: verifies Testcontainers + Flyway infrastructure works.
 */
class IntegrationTestBaseTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void containerIsRunning() {
        assertTrue(PG.isRunning(), "PostgreSQL container should be running");
    }

    @Test
    void flywayMigrationsApplied() {
        // V003 creates current_branch_id() function
        Boolean result = jdbc.queryForObject(
                "SELECT (current_branch_id() IS NULL)::boolean", Boolean.class);
        assertNotNull(result);
        assertTrue(result, "current_branch_id() should return NULL when not set");
    }

    @Test
    void rlsInfrastructureExists() {
        // apply_rls_policy() helper from V003
        Integer count = jdbc.queryForObject(
                "SELECT count(*)::int FROM pg_proc WHERE proname = 'apply_rls_policy'",
                Integer.class);
        assertEquals(1, count, "apply_rls_policy() function should exist");
    }

    @Test
    void branchesTableExists() {
        // V002 creates branches table
        Integer count = jdbc.queryForObject(
                "SELECT count(*)::int FROM information_schema.tables "
                + "WHERE table_name = 'branches' AND table_schema = 'public'",
                Integer.class);
        assertEquals(1, count, "branches table should exist from V002");
    }

    @Test
    void allMigrationsSucceeded() {
        // Check Flyway history — all migrations should be successful
        Integer failed = jdbc.queryForObject(
                "SELECT count(*)::int FROM flyway_schema_history WHERE success = false",
                Integer.class);
        assertEquals(0, failed, "All Flyway migrations should have succeeded");
    }
}
