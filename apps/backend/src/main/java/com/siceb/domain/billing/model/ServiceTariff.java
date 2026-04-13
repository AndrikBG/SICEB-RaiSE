package com.siceb.domain.billing.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Temporal tariff entry for a service within a branch. Each row represents a price
 * effective from a given date. Active tariff is resolved as the row with
 * MAX(effective_from) WHERE effective_from <= NOW(). Rows are immutable — price
 * changes create new entries with future effective dates.
 */
@Entity
@Table(name = "service_tariffs")
public class ServiceTariff {

    @Id
    @Column(name = "tariff_id")
    private UUID tariffId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ServiceTariff() {}

    public static ServiceTariff create(UUID serviceId, UUID branchId, BigDecimal basePrice,
                                        Instant effectiveFrom, UUID createdBy) {
        ServiceTariff tariff = new ServiceTariff();
        tariff.tariffId = UUID.randomUUID();
        tariff.serviceId = serviceId;
        tariff.branchId = branchId;
        tariff.basePrice = basePrice;
        tariff.effectiveFrom = effectiveFrom;
        tariff.createdBy = createdBy;
        tariff.createdAt = Instant.now();
        return tariff;
    }

    public UUID getTariffId() { return tariffId; }
    public UUID getServiceId() { return serviceId; }
    public UUID getBranchId() { return branchId; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
