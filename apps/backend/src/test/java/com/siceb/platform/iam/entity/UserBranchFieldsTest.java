package com.siceb.platform.iam.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserBranchFieldsTest {

    @Test
    void lastActiveBranchIdDefaultsToNull() {
        Role role = new Role("Test", "test role");
        User user = new User("testuser", "test@test.mx", "Test User", "hash", role, UUID.randomUUID());
        assertNull(user.getLastActiveBranchId());
    }

    @Test
    void setLastActiveBranchIdPersists() {
        Role role = new Role("Test", "test role");
        User user = new User("testuser", "test@test.mx", "Test User", "hash", role, UUID.randomUUID());
        UUID branchId = UUID.randomUUID();

        user.setLastActiveBranchId(branchId);
        assertEquals(branchId, user.getLastActiveBranchId());
    }
}
