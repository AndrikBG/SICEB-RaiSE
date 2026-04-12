package com.siceb.platform.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void emptyByDefault() {
        assertTrue(TenantContext.get().isEmpty());
    }

    @Test
    void setAndGet() {
        UUID id = UUID.randomUUID();
        TenantContext.set(id);
        assertEquals(id, TenantContext.get().orElseThrow());
    }

    @Test
    void clearRemovesValue() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertTrue(TenantContext.get().isEmpty());
    }

    @Test
    void requireThrowsWhenEmpty() {
        assertThrows(IllegalStateException.class, TenantContext::require);
    }

    @Test
    void requireReturnsValueWhenSet() {
        UUID id = UUID.randomUUID();
        TenantContext.set(id);
        assertEquals(id, TenantContext.require());
    }

    @Test
    void threadIsolation() throws Exception {
        UUID mainId = UUID.randomUUID();
        TenantContext.set(mainId);

        Thread other = new Thread(() -> assertTrue(TenantContext.get().isEmpty()));
        other.start();
        other.join();

        assertEquals(mainId, TenantContext.get().orElseThrow());
    }
}
