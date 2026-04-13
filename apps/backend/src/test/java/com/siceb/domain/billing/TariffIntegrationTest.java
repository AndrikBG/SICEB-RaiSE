package com.siceb.domain.billing;

import com.siceb.domain.billing.model.ServiceTariff;
import com.siceb.domain.billing.repository.TariffRepository;
import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TariffManagementService and TariffRepository
 * with real PostgreSQL (Testcontainers).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TariffIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private static UUID branchId;
    private static UUID serviceId;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeAll
    static void createTestData(@Autowired JdbcTemplate jdbc) {
        branchId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branches (id, name, address, is_active, onboarding_complete) VALUES (?, ?, ?, ?, ?)",
                branchId, "Tariff Integration Branch", "789 Int Ave", true, true);

        serviceId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                serviceId, branchId, "Consulta Especialidad", "CE01", true);
    }

    @Test
    @Order(1)
    void shouldSaveAndRetrieveTariff() {
        ServiceTariff tariff = ServiceTariff.create(
                serviceId, branchId, new BigDecimal("150.0000"),
                Instant.parse("2026-05-01T00:00:00Z"), USER_ID);

        ServiceTariff saved = tariffRepository.save(tariff);
        assertNotNull(saved.getTariffId());

        Optional<ServiceTariff> found = tariffRepository.findById(saved.getTariffId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("150.0000").compareTo(found.get().getBasePrice()));
    }

    @Test
    @Order(2)
    void shouldResolveActiveTariffWithTemporalData() {
        // Create multiple tariffs with different effective dates
        UUID svc = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                svc, branchId, "Consulta Temporal", "CT01", true);

        tariffRepository.save(ServiceTariff.create(
                svc, branchId, new BigDecimal("100.0000"),
                Instant.parse("2026-01-01T00:00:00Z"), USER_ID));
        tariffRepository.save(ServiceTariff.create(
                svc, branchId, new BigDecimal("150.0000"),
                Instant.parse("2026-04-01T00:00:00Z"), USER_ID));
        tariffRepository.save(ServiceTariff.create(
                svc, branchId, new BigDecimal("200.0000"),
                Instant.parse("2026-07-01T00:00:00Z"), USER_ID));

        // On 2026-05-15, active should be the one from 2026-04-01
        Optional<ServiceTariff> active = tariffRepository.findActiveTariff(
                svc, branchId, Instant.parse("2026-05-15T00:00:00Z"));

        assertTrue(active.isPresent());
        assertEquals(0, new BigDecimal("150.0000").compareTo(active.get().getBasePrice()));
        assertEquals(Instant.parse("2026-04-01T00:00:00Z"), active.get().getEffectiveFrom());
    }

    @Test
    @Order(3)
    void shouldReturnEmptyWhenNoTariffEffectiveYet() {
        UUID svc = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                svc, branchId, "Consulta Futura", "CF01", true);

        tariffRepository.save(ServiceTariff.create(
                svc, branchId, new BigDecimal("100.0000"),
                Instant.parse("2027-01-01T00:00:00Z"), USER_ID));

        // Query before any tariff is effective
        Optional<ServiceTariff> active = tariffRepository.findActiveTariff(
                svc, branchId, Instant.parse("2026-06-01T00:00:00Z"));

        assertTrue(active.isEmpty());
    }

    @Test
    @Order(4)
    void shouldSearchByServiceName() {
        var page = tariffRepository.searchByServiceName("Especialidad", branchId,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertFalse(page.isEmpty());
        assertTrue(page.getContent().stream()
                .allMatch(t -> t.getServiceId().equals(serviceId)));
    }

    @Test
    @Order(5)
    void shouldCheckDuplicateEffectiveDate() {
        boolean exists = tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(
                serviceId, branchId, Instant.parse("2026-05-01T00:00:00Z"));
        assertTrue(exists);

        boolean notExists = tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(
                serviceId, branchId, Instant.parse("2099-01-01T00:00:00Z"));
        assertFalse(notExists);
    }
}
