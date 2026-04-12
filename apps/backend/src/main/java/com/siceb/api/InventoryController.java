package com.siceb.api;

import com.siceb.domain.inventory.command.*;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.model.StockStatus;
import com.siceb.domain.inventory.service.InventoryCommandHandler;
import com.siceb.domain.inventory.service.InventoryExportService;
import com.siceb.domain.inventory.service.InventoryQueryService;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.shared.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryCommandHandler commandHandler;
    private final InventoryQueryService queryService;
    private final InventoryExportService exportService;
    private final AuthorizationService authorizationService;

    public InventoryController(InventoryCommandHandler commandHandler,
                                InventoryQueryService queryService,
                                InventoryExportService exportService,
                                AuthorizationService authorizationService) {
        this.commandHandler = commandHandler;
        this.queryService = queryService;
        this.exportService = exportService;
        this.authorizationService = authorizationService;
    }

    // ---- Command endpoints ----

    @PostMapping("/increments")
    @PreAuthorize("@auth.check('inventory:manage')")
    public ResponseEntity<DeltaResponse> incrementStock(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody IncrementRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        var cmd = new IncrementStockCommand(
                request.itemId(), request.quantity(), request.reason(), request.sourceRef(), idempotencyKey);
        InventoryDelta delta = commandHandler.handle(cmd, principal.activeBranchId(), principal.staffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeltaResponse.from(delta));
    }

    @PostMapping("/decrements")
    @PreAuthorize("@auth.check('inventory:manage')")
    public ResponseEntity<DeltaResponse> decrementStock(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DecrementRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        var cmd = new DecrementStockCommand(
                request.itemId(), request.quantity(), request.reason(), request.sourceRef(), idempotencyKey);
        InventoryDelta delta = commandHandler.handle(cmd, principal.activeBranchId(), principal.staffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeltaResponse.from(delta));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("@auth.check('inventory:adjust')")
    public ResponseEntity<DeltaResponse> adjustStock(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AdjustRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        var cmd = new AdjustStockCommand(
                request.itemId(), request.absoluteQuantity(), request.reason(), idempotencyKey);
        InventoryDelta delta = commandHandler.handle(cmd, principal.activeBranchId(), principal.staffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeltaResponse.from(delta));
    }

    @PutMapping("/{itemId}/threshold")
    @PreAuthorize("@auth.check('inventory:manage')")
    public ResponseEntity<DeltaResponse> setThreshold(
            @PathVariable UUID itemId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ThresholdRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        var cmd = new SetThresholdCommand(itemId, request.threshold(), idempotencyKey);
        InventoryDelta delta = commandHandler.handle(cmd, principal.activeBranchId(), principal.staffId());
        return ResponseEntity.ok(DeltaResponse.from(delta));
    }

    @PutMapping("/{itemId}/expiration")
    @PreAuthorize("@auth.check('inventory:manage')")
    public ResponseEntity<DeltaResponse> updateExpiration(
            @PathVariable UUID itemId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ExpirationRequest request) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        var cmd = new UpdateExpirationCommand(itemId, request.expirationDate(), idempotencyKey);
        InventoryDelta delta = commandHandler.handle(cmd, principal.activeBranchId(), principal.staffId());
        return ResponseEntity.ok(DeltaResponse.from(delta));
    }

    // ---- Query endpoints ----

    @GetMapping
    @PreAuthorize("@auth.check('inventory:read_service') or @auth.check('inventory:read_all')")
    public ResponseEntity<InventoryPageResponse> listInventory(
            @RequestParam(required = false) StockStatus filterStatus,
            @RequestParam(required = false) String filterCategory,
            @RequestParam(required = false) UUID filterService,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        UUID branchId = principal.activeBranchId();
        PageRequest pageable = PageRequest.of(page, size);

        Page<InventoryItem> result;
        if (search != null && !search.isBlank()) {
            result = queryService.searchItems(branchId, search, pageable);
        } else if (filterStatus != null) {
            result = queryService.findByBranchAndStatus(branchId, filterStatus, pageable);
        } else if (filterCategory != null) {
            result = queryService.findByBranchAndCategory(branchId, filterCategory, pageable);
        } else if (filterService != null) {
            result = queryService.findByBranchAndService(branchId, filterService, pageable);
        } else {
            result = queryService.findByBranch(branchId, pageable);
        }

        return ResponseEntity.ok(InventoryPageResponse.from(result));
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("@auth.check('inventory:read_service') or @auth.check('inventory:read_all')")
    public ResponseEntity<ItemResponse> getItem(@PathVariable UUID itemId) {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        InventoryItem item = queryService.findItem(itemId, principal.activeBranchId())
                .orElseThrow(() -> new InventoryException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Inventory item not found: " + itemId));
        return ResponseEntity.ok(ItemResponse.from(item));
    }

    @GetMapping("/export")
    @PreAuthorize("@auth.check('inventory:read_all')")
    public void exportInventory(HttpServletResponse response) throws IOException {
        SicebUserPrincipal principal = authorizationService.currentPrincipal();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventory.xlsx");
        exportService.exportToExcel(principal.activeBranchId(), response.getOutputStream());
    }

    // ---- Request DTOs ----

    public record IncrementRequest(
            @NotNull UUID itemId,
            @Positive int quantity,
            String reason,
            String sourceRef
    ) {}

    public record DecrementRequest(
            @NotNull UUID itemId,
            @Positive int quantity,
            String reason,
            String sourceRef
    ) {}

    public record AdjustRequest(
            @NotNull UUID itemId,
            int absoluteQuantity,
            @NotNull String reason
    ) {}

    public record ThresholdRequest(
            int threshold
    ) {}

    public record ExpirationRequest(
            @NotNull LocalDate expirationDate
    ) {}

    // ---- Response DTOs ----

    public record DeltaResponse(
            UUID deltaId,
            UUID itemId,
            String deltaType,
            Integer quantityChange,
            Integer absoluteQuantity
    ) {
        static DeltaResponse from(InventoryDelta d) {
            return new DeltaResponse(
                    d.getDeltaId(), d.getItemId(), d.getDeltaType().name(),
                    d.getQuantityChange(), d.getAbsoluteQuantity());
        }
    }

    public record ItemResponse(
            UUID itemId,
            UUID branchId,
            String sku,
            String name,
            String category,
            UUID serviceId,
            int currentStock,
            int minThreshold,
            String unitOfMeasure,
            LocalDate expirationDate,
            String stockStatus,
            String expirationStatus
    ) {
        static ItemResponse from(InventoryItem i) {
            return new ItemResponse(
                    i.getItemId(), i.getBranchId(), i.getSku(), i.getName(), i.getCategory(),
                    i.getServiceId(), i.getCurrentStock(), i.getMinThreshold(), i.getUnitOfMeasure(),
                    i.getExpirationDate(), i.getStockStatus().name(), i.getExpirationStatus().name());
        }
    }

    public record InventoryPageResponse(
            java.util.List<ItemResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        static InventoryPageResponse from(Page<InventoryItem> p) {
            return new InventoryPageResponse(
                    p.getContent().stream().map(ItemResponse::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
