package com.siceb.domain.inventory;

import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for V016 inventory schema: partitioning, materialization trigger,
 * pg_notify, append-only enforcement, RLS, and permissions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventorySchemaIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static UUID branchId;
    private static UUID serviceId;
    private static UUID itemId;
    private static final UUID STAFF_ID = UUID.randomUUID();

    @BeforeAll
    static void createTestBranchAndPartitions(@Autowired JdbcTemplate jdbc) {
        // Insert a branch for testing
        branchId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branches (id, name, address, is_active, onboarding_complete) VALUES (?, ?, ?, ?, ?)",
                branchId, "Test Branch", "123 Test St", true, true);

        // Create partitions for this branch
        jdbc.execute("SELECT create_inventory_partitions('" + branchId + "'::uuid)");

        // Set tenant context for RLS
        jdbc.execute("SET LOCAL app.branch_id = '" + branchId + "'");

        // Create a service catalog entry
        serviceId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) VALUES (?, ?, ?, ?, ?)",
                serviceId, branchId, "Pharmacy", "PHARM", true);

        // Create an inventory item
        itemId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO inventory_items (item_id, branch_id, sku, name, category, service_id, current_stock, min_threshold) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                itemId, branchId, "MED001", "Paracetamol 500mg", "medication", serviceId, 100, 20);
    }

    @Test
    @Order(1)
    void partitionsCreated() {
        String suffix = branchId.toString().replace("-", "_");

        Integer itemsPartition = jdbc.queryForObject(
                "SELECT count(*)::int FROM pg_class WHERE relname = 'inventory_items_" + suffix + "'",
                Integer.class);
        assertEquals(1, itemsPartition, "inventory_items partition should exist");

        Integer deltasPartition = jdbc.queryForObject(
                "SELECT count(*)::int FROM pg_class WHERE relname = 'inventory_deltas_" + suffix + "'",
                Integer.class);
        assertEquals(1, deltasPartition, "inventory_deltas partition should exist");
    }

    @Test
    @Order(2)
    void partitionCreationIsIdempotent() {
        // Calling again should not throw
        assertDoesNotThrow(() ->
                jdbc.execute("SELECT create_inventory_partitions('" + branchId + "'::uuid)"));
    }

    @Test
    @Order(3)
    void incrementMaterializesStock() {
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'INCREMENT', 50, ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "inc-test-001");

        Integer stock = jdbc.queryForObject(
                "SELECT current_stock FROM inventory_items WHERE item_id = ? AND branch_id = ?",
                Integer.class, itemId, branchId);
        assertEquals(150, stock, "Stock should be 100 + 50 = 150");
    }

    @Test
    @Order(4)
    void decrementMaterializesStock() {
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'DECREMENT', 30, ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "dec-test-001");

        Integer stock = jdbc.queryForObject(
                "SELECT current_stock FROM inventory_items WHERE item_id = ? AND branch_id = ?",
                Integer.class, itemId, branchId);
        assertEquals(120, stock, "Stock should be 150 - 30 = 120");
    }

    @Test
    @Order(5)
    void decrementBelowZeroRaisesException() {
        Exception ex = assertThrows(Exception.class, () ->
                jdbc.update(
                        "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                        + "VALUES (?, ?, ?, 'DECREMENT', 999, ?, ?)",
                        UUID.randomUUID(), itemId, branchId, STAFF_ID, "dec-test-overflow"));

        assertTrue(ex.getMessage().contains("Insufficient stock"),
                "Should raise insufficient stock error, got: " + ex.getMessage());
    }

    @Test
    @Order(6)
    void adjustSetsAbsoluteStock() {
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, absolute_quantity, reason, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'ADJUST', 200, 'Physical count', ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "adj-test-001");

        Integer stock = jdbc.queryForObject(
                "SELECT current_stock FROM inventory_items WHERE item_id = ? AND branch_id = ?",
                Integer.class, itemId, branchId);
        assertEquals(200, stock, "Stock should be adjusted to 200");
    }

    @Test
    @Order(7)
    void thresholdUpdatesMinAndStatus() {
        // Set threshold high to trigger LOW_STOCK
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, absolute_quantity, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'THRESHOLD', 500, ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "thr-test-001");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT min_threshold, stock_status FROM inventory_items WHERE item_id = ? AND branch_id = ?",
                itemId, branchId);
        assertEquals(500, row.get("min_threshold"), "Threshold should be 500");
        assertEquals("LOW_STOCK", row.get("stock_status"), "Status should be LOW_STOCK (200 < 500)");
    }

    @Test
    @Order(8)
    void outOfStockStatus() {
        // Adjust to 0
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, absolute_quantity, reason, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'ADJUST', 0, 'Depleted', ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "adj-test-zero");

        String status = jdbc.queryForObject(
                "SELECT stock_status FROM inventory_items WHERE item_id = ? AND branch_id = ?",
                String.class, itemId, branchId);
        assertEquals("OUT_OF_STOCK", status);
    }

    @Test
    @Order(9)
    void appendOnlyEnforced_updateBlocked() {
        // First restore some stock so the table has deltas
        Exception ex = assertThrows(Exception.class, () ->
                jdbc.update("UPDATE inventory_deltas SET reason = 'tampered' WHERE branch_id = ?", branchId));

        assertTrue(ex.getMessage().contains("append-only"),
                "UPDATE on inventory_deltas should be blocked, got: " + ex.getMessage());
    }

    @Test
    @Order(10)
    void appendOnlyEnforced_deleteBlocked() {
        Exception ex = assertThrows(Exception.class, () ->
                jdbc.update("DELETE FROM inventory_deltas WHERE branch_id = ?", branchId));

        assertTrue(ex.getMessage().contains("append-only"),
                "DELETE on inventory_deltas should be blocked, got: " + ex.getMessage());
    }

    @Test
    @Order(11)
    void pgNotifyFires() throws Exception {
        // Restore stock first
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, absolute_quantity, reason, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'ADJUST', 100, 'Restore', ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "adj-restore-for-notify");

        // Listen for notifications on a separate connection
        CopyOnWriteArrayList<String> notifications = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread listener = new Thread(() -> {
            try (Connection conn = PG.createConnection("")) {
                conn.createStatement().execute("LISTEN inventory_changes");
                // Poll for notification
                for (int i = 0; i < 50; i++) {
                    var pgConn = conn.unwrap(org.postgresql.PGConnection.class);
                    var notifs = pgConn.getNotifications(100);
                    if (notifs != null) {
                        for (var n : notifs) {
                            notifications.add(n.getParameter());
                            latch.countDown();
                        }
                    }
                    if (latch.getCount() == 0) break;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        listener.setDaemon(true);
        listener.start();

        // Small delay to ensure LISTEN is active
        Thread.sleep(200);

        // Insert a delta — should fire pg_notify
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'INCREMENT', 10, ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, "inc-notify-test");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive pg_notify within 5 seconds");
        assertFalse(notifications.isEmpty(), "Should have received at least one notification");

        String payload = notifications.get(0);
        assertTrue(payload.contains("branchId"), "Payload should contain branchId");
        assertTrue(payload.contains("itemId"), "Payload should contain itemId");
        assertTrue(payload.contains("INCREMENT"), "Payload should contain delta type");
    }

    @Test
    @Order(12)
    void permissionsSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*)::int FROM permissions WHERE key IN ('inventory:read_all', 'inventory:read_service', 'inventory:adjust')",
                Integer.class);
        assertEquals(3, count, "3 new inventory permissions should be seeded");
    }

    @Test
    @Order(13)
    void adminReportingGrantsExist() {
        // Verify admin_reporting can SELECT from inventory tables
        // This checks that GRANT SELECT was issued
        Integer count = jdbc.queryForObject(
                "SELECT count(*)::int FROM information_schema.role_table_grants "
                + "WHERE grantee = 'admin_reporting' AND table_name = 'inventory_items' AND privilege_type = 'SELECT'",
                Integer.class);
        assertEquals(1, count, "admin_reporting should have SELECT on inventory_items");
    }

    @Test
    @Order(14)
    void rlsPoliciesApplied() {
        // Verify RLS is enabled on inventory_items
        Boolean rlsEnabled = jdbc.queryForObject(
                "SELECT relforcerowsecurity FROM pg_class WHERE relname = 'inventory_items'",
                Boolean.class);
        assertNotNull(rlsEnabled);
        assertTrue(rlsEnabled, "RLS should be forced on inventory_items");
    }

    @Test
    @Order(15)
    void idempotencyKeyEnforcedGlobally() {
        String key = "idempotency-unique-test";
        jdbc.update(
                "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                + "VALUES (?, ?, ?, 'INCREMENT', 1, ?, ?)",
                UUID.randomUUID(), itemId, branchId, STAFF_ID, key);

        Exception ex = assertThrows(Exception.class, () ->
                jdbc.update(
                        "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, quantity_change, staff_id, idempotency_key) "
                        + "VALUES (?, ?, ?, 'INCREMENT', 1, ?, ?)",
                        UUID.randomUUID(), itemId, branchId, STAFF_ID, key));

        assertTrue(ex.getMessage().contains("uq_delta_idempotency") || ex.getMessage().contains("duplicate key"),
                "Duplicate idempotency key should be rejected");
    }
}
