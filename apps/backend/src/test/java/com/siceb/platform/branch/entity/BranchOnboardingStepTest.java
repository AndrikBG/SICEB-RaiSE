package com.siceb.platform.branch.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BranchOnboardingStepTest {

    @Test
    void createsPendingStep() {
        UUID branchId = UUID.randomUUID();
        BranchOnboardingStep step = new BranchOnboardingStep(branchId, "PARTITION_SETUP");

        assertEquals(branchId, step.getBranchId());
        assertEquals("PARTITION_SETUP", step.getStepName());
        assertEquals("PENDING", step.getStatus());
        assertNull(step.getCompletedAt());
        assertNull(step.getErrorMessage());
        assertNotNull(step.getId());
    }

    @Test
    void markInProgressChangesStatus() {
        BranchOnboardingStep step = new BranchOnboardingStep(UUID.randomUUID(), "SERVICE_CATALOG");
        step.markInProgress();
        assertEquals("IN_PROGRESS", step.getStatus());
    }

    @Test
    void markCompletedSetsTimestamp() {
        BranchOnboardingStep step = new BranchOnboardingStep(UUID.randomUUID(), "FINALIZE");
        step.markCompleted();
        assertEquals("COMPLETED", step.getStatus());
        assertNotNull(step.getCompletedAt());
    }

    @Test
    void markFailedRecordsError() {
        BranchOnboardingStep step = new BranchOnboardingStep(UUID.randomUUID(), "TARIFF_COPY");
        step.markFailed("Connection timeout");
        assertEquals("FAILED", step.getStatus());
        assertEquals("Connection timeout", step.getErrorMessage());
    }

    @Test
    void isCompletedReturnsTrueOnlyWhenCompleted() {
        BranchOnboardingStep step = new BranchOnboardingStep(UUID.randomUUID(), "TEST");
        assertFalse(step.isCompleted());
        step.markCompleted();
        assertTrue(step.isCompleted());
    }

    @Test
    void needsExecutionForPendingAndFailed() {
        BranchOnboardingStep pending = new BranchOnboardingStep(UUID.randomUUID(), "A");
        assertTrue(pending.needsExecution());

        BranchOnboardingStep failed = new BranchOnboardingStep(UUID.randomUUID(), "B");
        failed.markFailed("err");
        assertTrue(failed.needsExecution());

        BranchOnboardingStep completed = new BranchOnboardingStep(UUID.randomUUID(), "C");
        completed.markCompleted();
        assertFalse(completed.needsExecution());
    }
}
