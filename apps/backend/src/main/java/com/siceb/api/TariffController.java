package com.siceb.api;

import com.siceb.domain.billing.model.ServiceTariff;
import com.siceb.domain.billing.service.TariffManagementService;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    private final TariffManagementService tariffService;
    private final AuthorizationService authorizationService;

    public TariffController(TariffManagementService tariffService,
                             AuthorizationService authorizationService) {
        this.tariffService = tariffService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @PreAuthorize("@auth.check('tariff:manage')")
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody CreateTariffRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        ServiceTariff tariff = tariffService.createTariff(
                request.serviceId(), principal.activeBranchId(),
                request.basePrice(), request.effectiveFrom(), principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffResponse.from(tariff));
    }

    @PutMapping("/{tariffId}")
    @PreAuthorize("@auth.check('tariff:manage')")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable UUID tariffId,
            @Valid @RequestBody UpdateTariffRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        ServiceTariff tariff = tariffService.updateTariff(
                tariffId, request.basePrice(), request.effectiveFrom(), principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffResponse.from(tariff));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('tariff:read','tariff:manage')")
    public ResponseEntity<TariffResponse> getActiveTariff(@RequestParam UUID serviceId) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        return tariffService.getActiveTariff(serviceId, principal.activeBranchId())
                .map(t -> ResponseEntity.ok(TariffResponse.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('tariff:read','tariff:manage')")
    public ResponseEntity<TariffPageResponse> listTariffs(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(defaultValue = "true") boolean includeHistorical,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        Page<ServiceTariff> result = tariffService.listTariffs(
                principal.activeBranchId(), serviceId, includeHistorical, PageRequest.of(page, size));
        return ResponseEntity.ok(TariffPageResponse.from(result));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('tariff:read','tariff:manage')")
    public ResponseEntity<TariffPageResponse> searchTariffs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        Page<ServiceTariff> result = tariffService.searchTariffs(
                query, principal.activeBranchId(), PageRequest.of(page, size));
        return ResponseEntity.ok(TariffPageResponse.from(result));
    }

    // ---- Request DTOs ----

    public record CreateTariffRequest(
            @NotNull UUID serviceId,
            @NotNull BigDecimal basePrice,
            @NotNull Instant effectiveFrom
    ) {}

    public record UpdateTariffRequest(
            @NotNull BigDecimal basePrice,
            @NotNull Instant effectiveFrom
    ) {}

    // ---- Response DTOs ----

    public record TariffResponse(
            UUID tariffId,
            UUID serviceId,
            UUID branchId,
            BigDecimal basePrice,
            Instant effectiveFrom,
            UUID createdBy,
            Instant createdAt
    ) {
        static TariffResponse from(ServiceTariff t) {
            return new TariffResponse(
                    t.getTariffId(), t.getServiceId(), t.getBranchId(),
                    t.getBasePrice(), t.getEffectiveFrom(),
                    t.getCreatedBy(), t.getCreatedAt());
        }
    }

    public record TariffPageResponse(
            java.util.List<TariffResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        static TariffPageResponse from(Page<ServiceTariff> p) {
            return new TariffPageResponse(
                    p.getContent().stream().map(TariffResponse::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
