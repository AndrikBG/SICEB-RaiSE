package com.siceb.domain.billing.repository;

import com.siceb.domain.billing.model.ServiceTariff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffRepository extends JpaRepository<ServiceTariff, UUID> {

    /**
     * Resolve the active tariff: MAX(effective_from) WHERE effective_from <= now.
     */
    @Query("SELECT t FROM ServiceTariff t " +
           "WHERE t.serviceId = :serviceId AND t.branchId = :branchId " +
           "AND t.effectiveFrom <= :asOf " +
           "ORDER BY t.effectiveFrom DESC LIMIT 1")
    Optional<ServiceTariff> findActiveTariff(UUID serviceId, UUID branchId, Instant asOf);

    Page<ServiceTariff> findByBranchId(UUID branchId, Pageable pageable);

    Page<ServiceTariff> findByServiceIdAndBranchId(UUID serviceId, UUID branchId, Pageable pageable);

    @Query("SELECT t FROM ServiceTariff t " +
           "WHERE t.serviceId = :serviceId AND t.branchId = :branchId " +
           "AND t.effectiveFrom <= :asOf " +
           "ORDER BY t.effectiveFrom DESC")
    Page<ServiceTariff> findActiveOnly(UUID serviceId, UUID branchId, Instant asOf, Pageable pageable);

    /**
     * Search tariffs by service name via JOIN to branch_service_catalog.
     */
    @Query(value = "SELECT t.* FROM service_tariffs t " +
                   "JOIN branch_service_catalog c ON t.service_id = c.id " +
                   "WHERE c.service_name ILIKE '%' || :query || '%' " +
                   "AND t.branch_id = :branchId " +
                   "ORDER BY t.effective_from DESC",
           countQuery = "SELECT COUNT(*) FROM service_tariffs t " +
                        "JOIN branch_service_catalog c ON t.service_id = c.id " +
                        "WHERE c.service_name ILIKE '%' || :query || '%' " +
                        "AND t.branch_id = :branchId",
           nativeQuery = true)
    Page<ServiceTariff> searchByServiceName(String query, UUID branchId, Pageable pageable);

    boolean existsByServiceIdAndBranchIdAndEffectiveFrom(UUID serviceId, UUID branchId, Instant effectiveFrom);
}
