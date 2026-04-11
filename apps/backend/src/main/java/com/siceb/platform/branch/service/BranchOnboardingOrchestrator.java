package com.siceb.platform.branch.service;

import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.entity.BranchOnboardingStep;
import com.siceb.platform.branch.entity.ServiceCatalogEntry;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchOnboardingStepRepository;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.branch.repository.ServiceCatalogRepository;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 5-step idempotent onboarding workflow for new branches.
 * Steps 1 (PARTITION_SETUP), 3 (TARIFF_COPY), and 4 (INVENTORY_INIT)
 * are stubs in S4.1 — actual implementation in S4.2/S4.3.
 */
@Service
public class BranchOnboardingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BranchOnboardingOrchestrator.class);

    private static final List<String> STEP_NAMES = List.of(
            "PARTITION_SETUP", "SERVICE_CATALOG", "TARIFF_COPY", "INVENTORY_INIT", "FINALIZE");

    private final BranchOnboardingStepRepository stepRepository;
    private final BranchRepository branchRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;

    public BranchOnboardingOrchestrator(
            BranchOnboardingStepRepository stepRepository,
            BranchRepository branchRepository,
            ServiceCatalogRepository serviceCatalogRepository) {
        this.stepRepository = stepRepository;
        this.branchRepository = branchRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Transactional
    public void startOnboarding(UUID branchId) {
        List<BranchOnboardingStep> steps = STEP_NAMES.stream()
                .map(name -> new BranchOnboardingStep(branchId, name))
                .toList();
        stepRepository.saveAll(steps);
        log.info("Onboarding initialized for branch={} with {} steps", branchId, steps.size());

        runOnboarding(branchId);
    }

    @Transactional
    public void runOnboarding(UUID branchId) {
        List<BranchOnboardingStep> steps = stepRepository.findByBranchIdOrderByCreatedAtAsc(branchId);

        for (BranchOnboardingStep step : steps) {
            if (!step.needsExecution()) continue;

            step.markInProgress();
            try {
                executeStep(step, branchId);
                step.markCompleted();
                stepRepository.save(step);
                log.info("Onboarding step completed: branch={}, step={}", branchId, step.getStepName());
            } catch (Exception e) {
                step.markFailed(e.getMessage());
                stepRepository.save(step);
                log.error("Onboarding step failed: branch={}, step={}", branchId, step.getStepName(), e);
                return; // Stop on first failure — resume later
            }
        }

        // All steps completed — finalize branch
        boolean allCompleted = steps.stream().allMatch(BranchOnboardingStep::isCompleted);
        if (allCompleted) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));
            if (!branch.isOnboardingComplete()) {
                branch.completeOnboarding();
                branchRepository.save(branch);
                log.info("Onboarding complete for branch={}", branchId);
            }
        }
    }

    private void executeStep(BranchOnboardingStep step, UUID branchId) {
        switch (step.getStepName()) {
            case "PARTITION_SETUP" -> executePartitionSetup(branchId);
            case "SERVICE_CATALOG" -> executeServiceCatalog(branchId);
            case "TARIFF_COPY" -> executeTariffCopy(branchId);
            case "INVENTORY_INIT" -> executeInventoryInit(branchId);
            case "FINALIZE" -> { /* No-op — finalization handled after loop */ }
            default -> log.warn("Unknown onboarding step: {}", step.getStepName());
        }
    }

    // Stub — actual DDL partition creation in S4.2
    private void executePartitionSetup(UUID branchId) {
        log.warn("PARTITION_SETUP is a stub in S4.1 — actual implementation in S4.2");
    }

    private void executeServiceCatalog(UUID branchId) {
        // Seed default service catalog from org template
        List<ServiceCatalogEntry> defaults = List.of(
                new ServiceCatalogEntry(branchId, "Consulta General", "CG"),
                new ServiceCatalogEntry(branchId, "Consulta de Especialidad", "CE"),
                new ServiceCatalogEntry(branchId, "Laboratorio Clínico", "LAB"),
                new ServiceCatalogEntry(branchId, "Farmacia", "FAR"),
                new ServiceCatalogEntry(branchId, "Urgencias", "URG"));
        serviceCatalogRepository.saveAll(defaults);
        log.info("Service catalog seeded for branch={} with {} entries", branchId, defaults.size());
    }

    // Stub — tariff table in S4.3
    private void executeTariffCopy(UUID branchId) {
        log.warn("TARIFF_COPY is a stub in S4.1 — actual implementation in S4.3");
    }

    // Stub — inventory tables in S4.2
    private void executeInventoryInit(UUID branchId) {
        log.warn("INVENTORY_INIT is a stub in S4.1 — actual implementation in S4.2");
    }
}
