package com.siceb.platform.branch.service;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.branch.command.RegisterBranchCommand;
import com.siceb.platform.branch.command.UpdateBranchCommand;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.branch.TenantContext;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BranchRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(BranchRegistrationService.class);

    private final BranchRepository branchRepository;
    private final BranchOnboardingOrchestrator onboardingOrchestrator;
    private final AuditEventReceiver auditEventReceiver;

    public BranchRegistrationService(
            BranchRepository branchRepository,
            BranchOnboardingOrchestrator onboardingOrchestrator,
            AuditEventReceiver auditEventReceiver) {
        this.branchRepository = branchRepository;
        this.onboardingOrchestrator = onboardingOrchestrator;
        this.auditEventReceiver = auditEventReceiver;
    }

    @Transactional
    public Branch registerBranch(RegisterBranchCommand cmd) {
        if (branchRepository.existsByName(cmd.name())) {
            throw new BranchException(ErrorCode.CONFLICT, "A branch with this name already exists");
        }

        Branch branch = Branch.builder()
                .name(cmd.name())
                .address(cmd.address())
                .phone(cmd.phone())
                .email(cmd.email())
                .openingTime(cmd.openingTime())
                .closingTime(cmd.closingTime())
                .branchCode(cmd.branchCode())
                .build();

        branch = branchRepository.save(branch);

        onboardingOrchestrator.startOnboarding(branch.getBranchId());

        log.info("Branch registered: name={}, id={}", branch.getName(), branch.getBranchId());
        emitAuditEvent("BRANCH_REGISTERED", branch.getBranchId(), branch.getName());

        return branch;
    }

    @Transactional
    public Branch updateBranch(UUID branchId, UpdateBranchCommand cmd) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));

        if (cmd.name() != null) branch.setName(cmd.name());
        if (cmd.address() != null) branch.setAddress(cmd.address());
        if (cmd.phone() != null) branch.setPhone(cmd.phone());
        if (cmd.email() != null) branch.setEmail(cmd.email());
        if (cmd.openingTime() != null) branch.setOpeningTime(cmd.openingTime());
        if (cmd.closingTime() != null) branch.setClosingTime(cmd.closingTime());

        return branchRepository.save(branch);
    }

    @Transactional
    public Branch deactivateBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));

        branch.deactivate();
        branch = branchRepository.save(branch);

        log.info("Branch deactivated: name={}, id={}", branch.getName(), branchId);
        emitAuditEvent("BRANCH_DEACTIVATED", branchId, branch.getName());

        return branch;
    }

    private void emitAuditEvent(String action, UUID targetBranchId, String branchName) {
        // Use the caller's tenant context (admin's active branch) for the audit record's branch_id FK.
        // The target branch goes in targetId (no FK constraint). This avoids FK violations when
        // the target branch is being created in the outer transaction (REQUIRES_NEW isolation).
        UUID contextBranchId = TenantContext.get().orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", action);
        payload.put("branchName", branchName);
        auditEventReceiver.recordSecurityEvent(
                new AuditEventReceiver.SecurityAuditEvent(
                        action, null, contextBranchId, "Branch", targetBranchId, null, null, payload));
    }
}
