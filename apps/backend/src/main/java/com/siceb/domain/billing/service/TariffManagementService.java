package com.siceb.domain.billing.service;

import com.siceb.domain.billing.exception.TariffException;
import com.siceb.domain.billing.model.ServiceTariff;
import com.siceb.domain.billing.repository.TariffRepository;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages service tariffs with temporal effective-date pattern (D-042).
 * Tariff rows are immutable — price changes create new entries with future effective dates.
 */
@Service
public class TariffManagementService {

    private static final Logger log = LoggerFactory.getLogger(TariffManagementService.class);

    private final TariffRepository tariffRepository;

    public TariffManagementService(TariffRepository tariffRepository) {
        this.tariffRepository = tariffRepository;
    }

    @Transactional
    public ServiceTariff createTariff(UUID serviceId, UUID branchId, BigDecimal basePrice,
                                       Instant effectiveFrom, UUID createdBy) {
        validatePrice(basePrice);

        if (tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(serviceId, branchId, effectiveFrom)) {
            throw new TariffException(ErrorCode.CONFLICT,
                    "Tariff already exists for this service, branch, and effective date");
        }

        ServiceTariff tariff = ServiceTariff.create(serviceId, branchId, basePrice, effectiveFrom, createdBy);
        log.info("Creating tariff for service={} branch={} price={} effectiveFrom={}",
                serviceId, branchId, basePrice, effectiveFrom);
        return tariffRepository.save(tariff);
    }

    @Transactional
    public ServiceTariff updateTariff(UUID tariffId, BigDecimal newPrice,
                                       Instant newEffectiveFrom, UUID updatedBy) {
        validatePrice(newPrice);

        ServiceTariff existing = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new TariffException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Tariff not found: " + tariffId));

        if (tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(
                existing.getServiceId(), existing.getBranchId(), newEffectiveFrom)) {
            throw new TariffException(ErrorCode.CONFLICT,
                    "Tariff already exists for this service, branch, and effective date");
        }

        // Immutable: create new entry, never mutate existing
        ServiceTariff updated = ServiceTariff.create(
                existing.getServiceId(), existing.getBranchId(),
                newPrice, newEffectiveFrom, updatedBy);

        log.info("Creating new tariff entry (update): service={} branch={} price={} effectiveFrom={}",
                existing.getServiceId(), existing.getBranchId(), newPrice, newEffectiveFrom);
        return tariffRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public Optional<ServiceTariff> getActiveTariff(UUID serviceId, UUID branchId) {
        return tariffRepository.findActiveTariff(serviceId, branchId, Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<ServiceTariff> listTariffs(UUID branchId, UUID serviceId,
                                            boolean includeHistorical, Pageable pageable) {
        if (serviceId != null) {
            if (includeHistorical) {
                return tariffRepository.findByServiceIdAndBranchId(serviceId, branchId, pageable);
            }
            return tariffRepository.findActiveOnly(serviceId, branchId, Instant.now(), pageable);
        }
        return tariffRepository.findByBranchId(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTariff> searchTariffs(String query, UUID branchId, Pageable pageable) {
        return tariffRepository.searchByServiceName(query, branchId, pageable);
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new TariffException(ErrorCode.VALIDATION_FAILED,
                    "Base price must be non-negative");
        }
    }
}
