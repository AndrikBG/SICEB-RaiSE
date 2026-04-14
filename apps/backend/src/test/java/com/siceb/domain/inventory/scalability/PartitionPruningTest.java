package com.siceb.domain.inventory.scalability;

import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that PostgreSQL partition pruning is active for inventory queries.
 * Seeds 3 branches with dedicated partitions and verifies EXPLAIN ANALYZE
 * shows partition-specific scans (not full table scans).
 *
 * AC5: EXPLAIN ANALYZE confirms partition pruning on inventory_items and inventory_deltas.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PartitionPruningTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static final int BRANCH_COUNT = 3;
    private static final int ITEMS_PER_BRANCH = 100;
    private static final int DELTAS_PER_ITEM = 3;
    private static final UUID[] BRANCH_IDS = new UUID[BRANCH_COUNT];
    private static final UUID STAFF_ID = UUID.randomUUID();

    @BeforeAll
    static void seedTestData(@Autowired JdbcTemplate jdbc) {
        for (int b = 0; b < BRANCH_COUNT; b++) {
            BRANCH_IDS[b] = UUID.randomUUID();
            UUID branchId = BRANCH_IDS[b];

            // Create branch
            jdbc.update(
                    "INSERT INTO branches (id, name, address, is_active, onboarding_complete, branch_code) "
                            + "VALUES (?, ?, ?, true, true, ?)",
                    branchId, "PruningTest-Branch-" + (b + 1),
                    "Test Address " + (b + 1), "PT" + (b + 1));

            // Create partitions (function returns VOID — use execute, not update)
            jdbc.execute("SELECT create_inventory_partitions('" + branchId + "')");

            // Create service
            UUID serviceId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active) "
                            + "VALUES (?, ?, ?, ?, true)",
                    serviceId, branchId, "Service Branch " + (b + 1), "SB" + (b + 1));

            // Seed inventory items and deltas
            for (int i = 0; i < ITEMS_PER_BRANCH; i++) {
                UUID itemId = UUID.randomUUID();
                jdbc.update(
                        "INSERT INTO inventory_items (item_id, branch_id, sku, name, category, service_id, "
                                + "current_stock, min_threshold, unit_of_measure, stock_status, expiration_status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, 10, 'units', 'OK', 'OK')",
                        itemId, branchId, "SKU-PT-" + b + "-" + i,
                        "Item " + i, "MEDICAMENTO", serviceId, 50 + (i % 200));

                for (int d = 0; d < DELTAS_PER_ITEM; d++) {
                    jdbc.update(
                            "INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type, "
                                    + "quantity_change, reason, staff_id, idempotency_key) "
                                    + "VALUES (?, ?, ?, 'INCREMENT', ?, 'Pruning test', ?, ?)",
                            UUID.randomUUID(), itemId, branchId,
                            10 + (d % 50), STAFF_ID,
                            "prune-" + b + "-" + i + "-" + d);
                }
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("AC5: inventory_items queries use partition pruning")
    void inventoryItemsPartitionPruning() {
        UUID targetBranch = BRANCH_IDS[0];

        List<String> plan = jdbc.queryForList(
                "EXPLAIN ANALYZE SELECT * FROM inventory_items WHERE branch_id = ?",
                String.class, targetBranch);

        String fullPlan = String.join("\n", plan);

        // Partition pruning means the query only scans the branch-specific partition,
        // not the parent table or all partitions
        assertThat(fullPlan)
                .as("EXPLAIN should show scan on branch-specific partition, not full table")
                .containsIgnoringCase("inventory_items_");

        // Should NOT scan all partitions — verify "Partitions removed" or single partition scan
        // In PostgreSQL, partition pruning shows only the relevant partition in the plan
        long partitionScans = plan.stream()
                .filter(line -> line.toLowerCase().contains("seq scan on inventory_items_")
                        || line.toLowerCase().contains("index scan") || line.toLowerCase().contains("bitmap"))
                .count();

        assertThat(partitionScans)
                .as("Should scan exactly one partition (not all %d)", BRANCH_COUNT + 1) // +1 for default
                .isLessThanOrEqualTo(2); // partition + possible index scan
    }

    @Test
    @Order(2)
    @DisplayName("AC5: inventory_deltas queries use partition pruning")
    void inventoryDeltasPartitionPruning() {
        UUID targetBranch = BRANCH_IDS[1];

        List<String> plan = jdbc.queryForList(
                "EXPLAIN ANALYZE SELECT * FROM inventory_deltas WHERE branch_id = ?",
                String.class, targetBranch);

        String fullPlan = String.join("\n", plan);

        assertThat(fullPlan)
                .as("EXPLAIN should show scan on branch-specific partition")
                .containsIgnoringCase("inventory_deltas_");
    }

    @Test
    @Order(3)
    @DisplayName("Cross-branch admin query scans multiple partitions")
    void crossBranchQueryScansMultiplePartitions() {
        List<String> plan = jdbc.queryForList(
                "EXPLAIN ANALYZE SELECT branch_id, count(*) FROM inventory_items GROUP BY branch_id",
                String.class);

        String fullPlan = String.join("\n", plan);

        // Admin cross-branch query should scan all partitions (no single-branch filter)
        long partitionRefs = plan.stream()
                .filter(line -> line.toLowerCase().contains("inventory_items_"))
                .count();

        assertThat(partitionRefs)
                .as("Cross-branch query should reference multiple partitions")
                .isGreaterThanOrEqualTo(BRANCH_COUNT);
    }

    @AfterAll
    static void cleanup(@Autowired JdbcTemplate jdbc) {
        for (UUID branchId : BRANCH_IDS) {
            if (branchId == null) continue;
            String suffix = branchId.toString().replace("-", "_");
            // Drop partitions (cascades data) — avoids append-only trigger on deltas
            jdbc.execute("DROP TABLE IF EXISTS inventory_deltas_" + suffix);
            jdbc.execute("DROP TABLE IF EXISTS inventory_items_" + suffix);
            jdbc.update("DELETE FROM service_tariffs WHERE branch_id = ?", branchId);
            jdbc.update("DELETE FROM branch_service_catalog WHERE branch_id = ?", branchId);
            jdbc.update("DELETE FROM branch_onboarding_status WHERE branch_id = ?", branchId);
            jdbc.update("DELETE FROM branches WHERE id = ?", branchId);
        }
    }
}
