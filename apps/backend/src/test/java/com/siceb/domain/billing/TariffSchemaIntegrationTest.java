package com.siceb.domain.billing;

import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for V018 tariff schema: table structure, RLS, UNIQUE constraint,
 * permissions seed, admin_reporting grants.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TariffSchemaIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static UUID branchId;
    private static UUID serviceId;
    private static final UUID STAFF_ID = UUID.randomUUID();

    @BeforeAll
    static void createTestData(@Autowired JdbcTemplate jdbc) {
        branchId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branches (id, name, address, is_active, onboarding_complete) VALUES (?, ?, ?, ?, ?)",
                branchId, "Tariff Test Branch", "456 Test Ave", true, true);

        serviceId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                serviceId, branchId, "Consulta General", "CG01", true);
    }

    @Test
    @Order(1)
    void tableShouldExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'service_tariffs'",
                Integer.class);
        assertEquals(1, count, "service_tariffs table should exist");
    }

    @Test
    @Order(2)
    void shouldInsertTariffWithDecimalPrecision() {
        UUID tariffId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("150.5050");

        jdbc.update(
                "INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                tariffId, serviceId, branchId, price, Timestamp.from(Instant.parse("2026-05-01T00:00:00Z")), STAFF_ID);

        BigDecimal stored = jdbc.queryForObject(
                "SELECT base_price FROM service_tariffs WHERE tariff_id = ?",
                BigDecimal.class, tariffId);
        assertEquals(0, price.compareTo(stored), "DECIMAL(19,4) precision should be preserved");
    }

    @Test
    @Order(3)
    void shouldEnforceUniqueConstraintOnServiceBranchEffectiveFrom() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        Timestamp effectiveFrom = Timestamp.from(Instant.parse("2026-06-01T00:00:00Z"));

        jdbc.update(
                "INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                t1, serviceId, branchId, new BigDecimal("100.0000"), effectiveFrom, STAFF_ID);

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update(
                        "INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                        t2, serviceId, branchId, new BigDecimal("200.0000"), effectiveFrom, STAFF_ID),
                "Duplicate (service_id, branch_id, effective_from) should be rejected");
    }

    @Test
    @Order(4)
    void shouldRejectNegativePrice() {
        UUID tariffId = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update(
                        "INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                        tariffId, serviceId, branchId, new BigDecimal("-10.0000"),
                        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")), STAFF_ID),
                "Negative price should be rejected by CHECK constraint");
    }

    @Test
    @Order(5)
    void shouldAcceptZeroPrice() {
        UUID tariffId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                jdbc.update(
                        "INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                        tariffId, serviceId, branchId, new BigDecimal("0.0000"),
                        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")), STAFF_ID),
                "Zero price (free service) should be accepted");
    }

    @Test
    @Order(6)
    void tariffReadPermissionShouldExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE key = 'tariff:read'",
                Integer.class);
        assertEquals(1, count, "tariff:read permission should be seeded");
    }

    @Test
    @Order(7)
    void tariffManagePermissionShouldExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE key = 'tariff:manage'",
                Integer.class);
        assertEquals(1, count, "tariff:manage permission should already exist from V010");
    }

    @Test
    @Order(8)
    void rlsPolicyShouldBeApplied() {
        // Verify RLS is enabled on the table
        Boolean rlsEnabled = jdbc.queryForObject(
                "SELECT relrowsecurity FROM pg_class WHERE relname = 'service_tariffs'",
                Boolean.class);
        assertTrue(rlsEnabled, "RLS should be enabled on service_tariffs");
    }

    @Test
    @Order(9)
    void adminReportingShouldHaveSelectGrant() {
        // Check that admin_reporting role has SELECT on service_tariffs
        Boolean hasGrant = jdbc.queryForObject(
                "SELECT has_table_privilege('admin_reporting', 'service_tariffs', 'SELECT')",
                Boolean.class);
        assertTrue(hasGrant, "admin_reporting should have SELECT on service_tariffs");
    }
}
