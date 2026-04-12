package com.siceb.platform.branch.service;

import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.entity.BranchOnboardingStep;
import com.siceb.platform.branch.repository.BranchOnboardingStepRepository;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.branch.repository.ServiceCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchOnboardingOrchestratorTest {

    @Mock private BranchOnboardingStepRepository stepRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private ServiceCatalogRepository serviceCatalogRepository;

    private BranchOnboardingOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new BranchOnboardingOrchestrator(stepRepository, branchRepository, serviceCatalogRepository);
    }

    @Test
    void startOnboarding_creates5PendingSteps() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.builder().name("Test").build();

        // startOnboarding calls runOnboarding internally
        when(stepRepository.findByBranchIdOrderByCreatedAtAsc(branchId)).thenReturn(List.of());
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        orchestrator.startOnboarding(branchId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BranchOnboardingStep>> captor = ArgumentCaptor.forClass(List.class);
        verify(stepRepository).saveAll(captor.capture());

        List<BranchOnboardingStep> steps = captor.getValue();
        assertEquals(5, steps.size());
        assertEquals("PARTITION_SETUP", steps.get(0).getStepName());
        assertEquals("SERVICE_CATALOG", steps.get(1).getStepName());
        assertEquals("TARIFF_COPY", steps.get(2).getStepName());
        assertEquals("INVENTORY_INIT", steps.get(3).getStepName());
        assertEquals("FINALIZE", steps.get(4).getStepName());
    }

    @Test
    void runOnboarding_allPending_completesAllSteps() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.builder().name("Test").build();

        List<BranchOnboardingStep> steps = List.of(
                new BranchOnboardingStep(branchId, "PARTITION_SETUP"),
                new BranchOnboardingStep(branchId, "SERVICE_CATALOG"),
                new BranchOnboardingStep(branchId, "TARIFF_COPY"),
                new BranchOnboardingStep(branchId, "INVENTORY_INIT"),
                new BranchOnboardingStep(branchId, "FINALIZE"));

        when(stepRepository.findByBranchIdOrderByCreatedAtAsc(branchId)).thenReturn(steps);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        orchestrator.runOnboarding(branchId);

        steps.forEach(s -> assertTrue(s.isCompleted(), "Step " + s.getStepName() + " should be completed"));
        assertTrue(branch.isOnboardingComplete());
        verify(branchRepository).save(branch);
    }

    @Test
    void runOnboarding_resumesFromFailedStep() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.builder().name("Test").build();

        BranchOnboardingStep completed = new BranchOnboardingStep(branchId, "PARTITION_SETUP");
        completed.markCompleted();
        BranchOnboardingStep failed = new BranchOnboardingStep(branchId, "SERVICE_CATALOG");
        failed.markFailed("timeout");
        BranchOnboardingStep pending = new BranchOnboardingStep(branchId, "TARIFF_COPY");
        BranchOnboardingStep pending2 = new BranchOnboardingStep(branchId, "INVENTORY_INIT");
        BranchOnboardingStep pending3 = new BranchOnboardingStep(branchId, "FINALIZE");

        when(stepRepository.findByBranchIdOrderByCreatedAtAsc(branchId))
                .thenReturn(List.of(completed, failed, pending, pending2, pending3));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        orchestrator.runOnboarding(branchId);

        assertTrue(completed.isCompleted());
        assertTrue(failed.isCompleted()); // was failed, now retried and completed
        assertTrue(pending.isCompleted());
        assertTrue(branch.isOnboardingComplete());
    }

    @Test
    void runOnboarding_idempotent_alreadyCompleted() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.builder().name("Test").build();
        branch.completeOnboarding();

        List<BranchOnboardingStep> steps = List.of(
                new BranchOnboardingStep(branchId, "PARTITION_SETUP"),
                new BranchOnboardingStep(branchId, "FINALIZE"));
        steps.forEach(BranchOnboardingStep::markCompleted);

        when(stepRepository.findByBranchIdOrderByCreatedAtAsc(branchId)).thenReturn(steps);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        // Should not throw, should be a no-op
        orchestrator.runOnboarding(branchId);

        // Branch already complete — no additional save needed for onboarding flag
        assertTrue(branch.isOnboardingComplete());
    }
}
