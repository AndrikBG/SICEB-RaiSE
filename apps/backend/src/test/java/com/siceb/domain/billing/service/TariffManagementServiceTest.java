package com.siceb.domain.billing.service;

import com.siceb.domain.billing.exception.TariffException;
import com.siceb.domain.billing.model.ServiceTariff;
import com.siceb.domain.billing.repository.TariffRepository;
import com.siceb.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffManagementServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    private TariffManagementService service;

    private final UUID serviceId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TariffManagementService(tariffRepository);
    }

    @Nested
    class CreateTariff {

        @Test
        void shouldCreateTariffWithValidInput() {
            BigDecimal price = new BigDecimal("150.0000");
            Instant effectiveFrom = Instant.parse("2027-01-01T00:00:00Z");

            when(tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(serviceId, branchId, effectiveFrom))
                    .thenReturn(false);
            when(tariffRepository.save(any(ServiceTariff.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ServiceTariff result = service.createTariff(serviceId, branchId, price, effectiveFrom, userId);

            assertNotNull(result);
            assertEquals(serviceId, result.getServiceId());
            assertEquals(branchId, result.getBranchId());
            assertEquals(0, price.compareTo(result.getBasePrice()));
            assertEquals(effectiveFrom, result.getEffectiveFrom());
            verify(tariffRepository).save(any(ServiceTariff.class));
        }

        @Test
        void shouldRejectNegativePrice() {
            BigDecimal negativePrice = new BigDecimal("-10.00");
            Instant effectiveFrom = Instant.parse("2027-01-01T00:00:00Z");

            TariffException ex = assertThrows(TariffException.class,
                    () -> service.createTariff(serviceId, branchId, negativePrice, effectiveFrom, userId));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }

        @Test
        void shouldAcceptZeroPrice() {
            BigDecimal zeroPrice = BigDecimal.ZERO;
            Instant effectiveFrom = Instant.parse("2027-01-01T00:00:00Z");

            when(tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(serviceId, branchId, effectiveFrom))
                    .thenReturn(false);
            when(tariffRepository.save(any(ServiceTariff.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ServiceTariff result = service.createTariff(serviceId, branchId, zeroPrice, effectiveFrom, userId);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getBasePrice()));
        }

        @Test
        void shouldRejectDuplicateEffectiveDate() {
            Instant effectiveFrom = Instant.parse("2027-01-01T00:00:00Z");

            when(tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(serviceId, branchId, effectiveFrom))
                    .thenReturn(true);

            TariffException ex = assertThrows(TariffException.class,
                    () -> service.createTariff(serviceId, branchId, new BigDecimal("100.00"), effectiveFrom, userId));
            assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        }
    }

    @Nested
    class UpdateTariff {

        @Test
        void shouldCreateNewEntryWithFutureEffectiveDate() {
            UUID existingTariffId = UUID.randomUUID();
            ServiceTariff existing = ServiceTariff.create(serviceId, branchId,
                    new BigDecimal("100.00"), Instant.parse("2026-01-01T00:00:00Z"), userId);

            when(tariffRepository.findById(existingTariffId)).thenReturn(Optional.of(existing));
            when(tariffRepository.existsByServiceIdAndBranchIdAndEffectiveFrom(any(), any(), any()))
                    .thenReturn(false);
            when(tariffRepository.save(any(ServiceTariff.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            BigDecimal newPrice = new BigDecimal("175.00");
            Instant futureDate = Instant.parse("2027-06-01T00:00:00Z");

            ServiceTariff result = service.updateTariff(existingTariffId, newPrice, futureDate, userId);

            // New entry created, not mutation of existing
            assertNotEquals(existingTariffId, result.getTariffId());
            assertEquals(serviceId, result.getServiceId());
            assertEquals(0, newPrice.compareTo(result.getBasePrice()));
            assertEquals(futureDate, result.getEffectiveFrom());
        }

        @Test
        void shouldRejectNonExistentTariff() {
            UUID nonExistent = UUID.randomUUID();
            when(tariffRepository.findById(nonExistent)).thenReturn(Optional.empty());

            TariffException ex = assertThrows(TariffException.class,
                    () -> service.updateTariff(nonExistent, new BigDecimal("100.00"),
                            Instant.parse("2027-01-01T00:00:00Z"), userId));
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    class ResolveActiveTariff {

        @Test
        void shouldResolveActiveTariff() {
            ServiceTariff active = ServiceTariff.create(serviceId, branchId,
                    new BigDecimal("150.00"), Instant.parse("2026-04-01T00:00:00Z"), userId);

            when(tariffRepository.findActiveTariff(eq(serviceId), eq(branchId), any(Instant.class)))
                    .thenReturn(Optional.of(active));

            Optional<ServiceTariff> result = service.getActiveTariff(serviceId, branchId);

            assertTrue(result.isPresent());
            assertEquals(0, new BigDecimal("150.00").compareTo(result.get().getBasePrice()));
        }

        @Test
        void shouldReturnEmptyWhenNoActiveTariff() {
            when(tariffRepository.findActiveTariff(eq(serviceId), eq(branchId), any(Instant.class)))
                    .thenReturn(Optional.empty());

            Optional<ServiceTariff> result = service.getActiveTariff(serviceId, branchId);
            assertTrue(result.isEmpty());
        }
    }
}
