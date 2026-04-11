package com.siceb.platform.branch.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCatalogEntryTest {

    @Test
    void createsActiveEntry() {
        UUID branchId = UUID.randomUUID();
        ServiceCatalogEntry entry = new ServiceCatalogEntry(branchId, "Consulta General", "CG-001");

        assertEquals(branchId, entry.getBranchId());
        assertEquals("Consulta General", entry.getServiceName());
        assertEquals("CG-001", entry.getServiceCode());
        assertTrue(entry.isActive());
        assertNotNull(entry.getId());
    }

    @Test
    void deactivateDisablesEntry() {
        ServiceCatalogEntry entry = new ServiceCatalogEntry(UUID.randomUUID(), "Laboratorio", "LAB");
        entry.deactivate();
        assertFalse(entry.isActive());
    }
}
