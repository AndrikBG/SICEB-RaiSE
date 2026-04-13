package com.siceb.api;

import com.siceb.domain.inventory.command.*;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.domain.inventory.model.DeltaType;
import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.model.StockStatus;
import com.siceb.domain.inventory.service.InventoryCommandHandler;
import com.siceb.domain.inventory.service.InventoryExportService;
import com.siceb.domain.inventory.service.InventoryQueryService;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock private InventoryCommandHandler commandHandler;
    @Mock private InventoryQueryService queryService;
    @Mock private InventoryExportService exportService;
    @Mock private AuthorizationService authorizationService;

    private InventoryController controller;

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID STAFF_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new InventoryController(commandHandler, queryService, exportService, authorizationService);
    }

    // ---- Command endpoints ----

    @Test
    void incrementStock_valid_returns201() {
        InventoryDelta delta = buildDelta(DeltaType.INCREMENT, 50, null);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(IncrementStockCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(delta);

        var request = new InventoryController.IncrementRequest(ITEM_ID, 50, "Delivery", "PO-001");
        var response = controller.incrementStock("idem-001", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(delta.getDeltaId(), response.getBody().deltaId());
    }

    @Test
    void decrementStock_valid_returns201() {
        InventoryDelta delta = buildDelta(DeltaType.DECREMENT, 10, null);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(DecrementStockCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(delta);

        var request = new InventoryController.DecrementRequest(ITEM_ID, 10, "Dispensation", "DISP-001");
        var response = controller.decrementStock("idem-002", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void decrementStock_insufficientStock_propagatesException() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(DecrementStockCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenThrow(new InventoryException(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock"));

        var request = new InventoryController.DecrementRequest(ITEM_ID, 200, "Dispensation", "DISP-002");

        InventoryException ex = assertThrows(InventoryException.class,
                () -> controller.decrementStock("idem-003", request));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    void adjustStock_valid_returns201() {
        InventoryDelta delta = buildDelta(DeltaType.ADJUST, null, 145);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(AdjustStockCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(delta);

        var request = new InventoryController.AdjustRequest(ITEM_ID, 145, "Physical count reconciliation");
        var response = controller.adjustStock("idem-004", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void setThreshold_valid_returns200() {
        InventoryDelta delta = buildDelta(DeltaType.THRESHOLD, null, 30);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(SetThresholdCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(delta);

        var request = new InventoryController.ThresholdRequest(30);
        var response = controller.setThreshold(ITEM_ID, "idem-005", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateExpiration_valid_returns200() {
        InventoryDelta delta = buildDelta(DeltaType.EXPIRATION, null, null);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(UpdateExpirationCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(delta);

        var request = new InventoryController.ExpirationRequest(java.time.LocalDate.of(2027, 6, 30));
        var response = controller.updateExpiration(ITEM_ID, "idem-006", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void duplicateIdempotencyKey_returnsExistingResult() {
        InventoryDelta existingDelta = buildDelta(DeltaType.INCREMENT, 50, null);
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(commandHandler.handle(any(IncrementStockCommand.class), eq(BRANCH_ID), eq(STAFF_ID)))
                .thenReturn(existingDelta);

        var request = new InventoryController.IncrementRequest(ITEM_ID, 50, "Delivery", "PO-001");
        var response = controller.incrementStock("idem-dup", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(existingDelta.getDeltaId(), response.getBody().deltaId());
    }

    // ---- Query endpoints ----

    @Test
    void listInventory_noFilters_returnsPaginatedItems() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        InventoryItem item = createItem("Paracetamol");
        Page<InventoryItem> page = new PageImpl<>(List.of(item), PageRequest.of(0, 50), 1);
        when(queryService.findByBranch(eq(BRANCH_ID), any())).thenReturn(page);

        var response = controller.listInventory(null, null, null, null, 0, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().totalElements());
    }

    @Test
    void listInventory_filterByStatus_delegates() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(queryService.findByBranchAndStatus(eq(BRANCH_ID), eq(StockStatus.LOW_STOCK), any())).thenReturn(page);

        var response = controller.listInventory(StockStatus.LOW_STOCK, null, null, null, 0, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(queryService).findByBranchAndStatus(eq(BRANCH_ID), eq(StockStatus.LOW_STOCK), any());
    }

    @Test
    void listInventory_search_delegates() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(queryService.searchItems(eq(BRANCH_ID), eq("paracet"), any())).thenReturn(page);

        var response = controller.listInventory(null, null, null, "paracet", 0, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(queryService).searchItems(eq(BRANCH_ID), eq("paracet"), any());
    }

    @Test
    void getItem_found_returns200() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        InventoryItem item = createItem("Insulin");
        when(queryService.findItem(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(item));

        var response = controller.getItem(ITEM_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Insulin", response.getBody().name());
    }

    @Test
    void getItem_notFound_throws() {
        when(authorizationService.currentPrincipal()).thenReturn(principal());
        when(queryService.findItem(ITEM_ID, BRANCH_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryException.class, () -> controller.getItem(ITEM_ID));
    }

    // ---- Helpers ----

    private SicebUserPrincipal principal() {
        return new SicebUserPrincipal(
                UUID.randomUUID(), "admin", "Admin User", "Administrador General", null,
                BRANCH_ID, Set.of("inventory:read_all", "inventory:adjust"), Set.of(BRANCH_ID.toString()), STAFF_ID);
    }

    private InventoryDelta buildDelta(DeltaType type, Integer qtyChange, Integer absQty) {
        return InventoryDelta.builder()
                .itemId(ITEM_ID)
                .branchId(BRANCH_ID)
                .deltaType(type)
                .quantityChange(qtyChange)
                .absoluteQuantity(absQty)
                .staffId(STAFF_ID)
                .idempotencyKey("test-key")
                .build();
    }

    private InventoryItem createItem(String name) {
        return InventoryItem.create(
                ITEM_ID, BRANCH_ID, "SKU-001", name, "PHARMA",
                UUID.randomUUID(), "units", 100, 10, null);
    }
}
