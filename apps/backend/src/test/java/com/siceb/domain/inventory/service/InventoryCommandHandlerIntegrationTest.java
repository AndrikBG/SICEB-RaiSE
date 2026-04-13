package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.command.AdjustStockCommand;
import com.siceb.domain.inventory.command.DecrementStockCommand;
import com.siceb.domain.inventory.command.IncrementStockCommand;
import com.siceb.domain.inventory.command.SetThresholdCommand;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.domain.inventory.model.DeltaType;
import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import com.siceb.shared.ErrorCode;
import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: verifies command handler + PG trigger materialization end-to-end.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryCommandHandlerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private InventoryCommandHandler handler;
    @Autowired
    private InventoryItemRepository itemRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private static UUID branchId;
    private static UUID serviceId;
    private static UUID itemId;
    private static final UUID STAFF_ID = UUID.randomUUID();

    @BeforeAll
    static void createTestData(@Autowired JdbcTemplate jdbc) {
        branchId = UUID.randomUUID();
        jdbc.update("INSERT INTO branches (id, name, address, is_active, onboarding_complete) VALUES (?, ?, ?, ?, ?)",
                branchId, "Integration Branch", "456 Test Rd", true, true);
        jdbc.execute("SELECT create_inventory_partitions('" + branchId + "'::uuid)");

        serviceId = UUID.randomUUID();
        jdbc.update("INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                serviceId, branchId, "Laboratory", "LAB", true);

        itemId = UUID.randomUUID();
        jdbc.update("INSERT INTO inventory_items (item_id, branch_id, sku, name, category, service_id, current_stock, min_threshold) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                itemId, branchId, "REA001", "Glucose Reagent", "reagent", serviceId, 50, 10);
    }

    @Test
    @Order(1)
    void incrementMaterializesViaHandler() {
        var cmd = new IncrementStockCommand(itemId, 25, "Delivery", "PO-INT-001", "int-inc-001");
        InventoryDelta delta = handler.handle(cmd, branchId, STAFF_ID);

        assertEquals(DeltaType.INCREMENT, delta.getDeltaType());

        // Verify trigger materialized the stock
        InventoryItem item = itemRepository.findByItemIdAndBranchId(itemId, branchId).orElseThrow();
        assertEquals(75, item.getCurrentStock(), "Stock should be 50 + 25 = 75");
    }

    @Test
    @Order(2)
    void decrementMaterializesViaHandler() {
        var cmd = new DecrementStockCommand(itemId, 20, "Usage", "DISP-INT-001", "int-dec-001");
        handler.handle(cmd, branchId, STAFF_ID);

        InventoryItem item = itemRepository.findByItemIdAndBranchId(itemId, branchId).orElseThrow();
        assertEquals(55, item.getCurrentStock(), "Stock should be 75 - 20 = 55");
    }

    @Test
    @Order(3)
    void decrementBelowZeroRejectedByTrigger() {
        var cmd = new DecrementStockCommand(itemId, 999, "Bad", null, "int-dec-overflow");

        // The PG trigger raises an exception caught as a DataAccessException
        assertThrows(Exception.class, () -> handler.handle(cmd, branchId, STAFF_ID));
    }

    @Test
    @Order(4)
    void adjustMaterializesViaHandler() {
        var cmd = new AdjustStockCommand(itemId, 100, "Physical count reconciliation", "int-adj-001");
        handler.handle(cmd, branchId, STAFF_ID);

        InventoryItem item = itemRepository.findByItemIdAndBranchId(itemId, branchId).orElseThrow();
        assertEquals(100, item.getCurrentStock());
    }

    @Test
    @Order(5)
    void thresholdUpdatesStatus() {
        var cmd = new SetThresholdCommand(itemId, 200, "int-thr-001");
        handler.handle(cmd, branchId, STAFF_ID);

        InventoryItem item = itemRepository.findByItemIdAndBranchId(itemId, branchId).orElseThrow();
        assertEquals(200, item.getMinThreshold());
        assertEquals("LOW_STOCK", item.getStockStatus().name(), "100 < 200 = LOW_STOCK");
    }

    @Test
    @Order(6)
    void idempotencyReturnsExistingDelta() {
        var cmd1 = new IncrementStockCommand(itemId, 10, "First", null, "int-idem-001");
        InventoryDelta first = handler.handle(cmd1, branchId, STAFF_ID);

        var cmd2 = new IncrementStockCommand(itemId, 10, "Duplicate", null, "int-idem-001");
        InventoryDelta second = handler.handle(cmd2, branchId, STAFF_ID);

        assertEquals(first.getDeltaId(), second.getDeltaId(), "Should return same delta");

        // Stock should reflect only one increment (not two)
        InventoryItem item = itemRepository.findByItemIdAndBranchId(itemId, branchId).orElseThrow();
        assertEquals(110, item.getCurrentStock(), "100 + 10 = 110 (not 120)");
    }

    @Test
    @Order(7)
    void missingItemRejected() {
        UUID fakeId = UUID.randomUUID();
        var cmd = new IncrementStockCommand(fakeId, 10, "Delivery", null, "int-nf-001");

        InventoryException ex = assertThrows(InventoryException.class,
                () -> handler.handle(cmd, branchId, STAFF_ID));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }
}
