package com.siceb.platform.realtime;

import com.siceb.test.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test: delta INSERT → pg_notify trigger → publisher → STOMP template.
 * Uses real PostgreSQL (Testcontainers) with mocked SimpMessagingTemplate.
 */
class RealtimeEventPublisherIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RealtimeEventPublisher publisher;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void deltaInsert_triggersNotification_publishedToStompTopics() {
        // Setup: create a branch partition and inventory item
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID serviceId = setupBranchAndItem(branchId, itemId, staffId);

        // Act: insert a delta — the PG trigger fires pg_notify
        jdbcTemplate.update("""
                INSERT INTO inventory_deltas (delta_id, item_id, branch_id, delta_type,
                    quantity_change, staff_id, idempotency_key)
                VALUES (?, ?, ?, 'INCREMENT', 25, ?, ?)
                """,
                UUID.randomUUID(), itemId, branchId, staffId,
                "idem-rt-" + UUID.randomUUID());

        // Poll notifications (direct call instead of waiting for @Scheduled)
        publisher.pollNotifications();

        // Assert: publisher sent to branch topic and admin topic
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/branch/" + branchId + "/inventory"), payloadCaptor.capture());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/admin/inventory"), payloadCaptor.capture());

        String payload = payloadCaptor.getAllValues().getFirst();
        assertTrue(payload.contains(branchId.toString()));
        assertTrue(payload.contains(itemId.toString()));
        assertTrue(payload.contains("INCREMENT"));
    }

    private UUID setupBranchAndItem(UUID branchId, UUID itemId, UUID staffId) {
        // Create branch
        jdbcTemplate.update("""
                INSERT INTO branches (id, name, address, phone, email, branch_code, is_active, onboarding_complete)
                VALUES (?, 'Test Branch RT', 'Addr', '555', 'rt@test.com', 'RT01', true, true)
                """, branchId);

        // Create partitions
        jdbcTemplate.execute(
                "SELECT create_inventory_partitions('" + branchId + "')");

        // Create service catalog entry
        UUID serviceId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO branch_service_catalog (id, branch_id, service_name, is_active)
                VALUES (?, ?, 'Test Service', true)
                """, serviceId, branchId);

        // Create inventory item
        jdbcTemplate.update("""
                INSERT INTO inventory_items (item_id, branch_id, sku, name, category,
                    service_id, current_stock, min_threshold, unit_of_measure,
                    stock_status, expiration_status)
                VALUES (?, ?, 'SKU-RT-001', 'Test Item RT', 'PHARMA',
                    ?, 100, 10, 'units', 'OK', 'OK')
                """, itemId, branchId, serviceId);

        return serviceId;
    }
}
