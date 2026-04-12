package com.siceb.platform.branch.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BranchTest {

    @Test
    void builderCreatesFullBranch() {
        Branch branch = Branch.builder()
                .name("Sucursal Norte")
                .address("Av. Universidad 1500")
                .phone("+528112345678")
                .email("norte@clinica.mx")
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(20, 0))
                .branchCode("NTE")
                .build();

        assertNotNull(branch.getBranchId());
        assertEquals("Sucursal Norte", branch.getName());
        assertEquals("Av. Universidad 1500", branch.getAddress());
        assertEquals("+528112345678", branch.getPhone());
        assertEquals("norte@clinica.mx", branch.getEmail());
        assertEquals(LocalTime.of(8, 0), branch.getOpeningTime());
        assertEquals(LocalTime.of(20, 0), branch.getClosingTime());
        assertEquals("NTE", branch.getBranchCode());
        assertTrue(branch.isActive());
        assertFalse(branch.isOnboardingComplete());
        assertNotNull(branch.getCreatedAt());
        assertNotNull(branch.getUpdatedAt());
    }

    @Test
    void builderRequiresName() {
        assertThrows(NullPointerException.class, () ->
                Branch.builder().address("test").build());
    }

    @Test
    void settersUpdateFields() {
        Branch branch = Branch.builder()
                .name("Original")
                .address("Addr")
                .build();

        branch.setName("Updated");
        branch.setAddress("New Addr");
        branch.setPhone("+521234567890");
        branch.setEmail("new@test.mx");
        branch.setOpeningTime(LocalTime.of(9, 0));
        branch.setClosingTime(LocalTime.of(18, 0));

        assertEquals("Updated", branch.getName());
        assertEquals("New Addr", branch.getAddress());
        assertEquals("+521234567890", branch.getPhone());
        assertEquals("new@test.mx", branch.getEmail());
        assertEquals(LocalTime.of(9, 0), branch.getOpeningTime());
        assertEquals(LocalTime.of(18, 0), branch.getClosingTime());
    }

    @Test
    void deactivateSetsInactive() {
        Branch branch = Branch.builder().name("Test").build();
        assertTrue(branch.isActive());

        branch.deactivate();
        assertFalse(branch.isActive());
    }

    @Test
    void completeOnboardingSetsFlag() {
        Branch branch = Branch.builder().name("Test").build();
        assertFalse(branch.isOnboardingComplete());

        branch.completeOnboarding();
        assertTrue(branch.isOnboardingComplete());
    }
}
