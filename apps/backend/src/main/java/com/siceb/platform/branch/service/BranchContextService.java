package com.siceb.platform.branch.service;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.iam.entity.MedicalStaff;
import com.siceb.platform.iam.entity.User;
import com.siceb.platform.iam.repository.MedicalStaffRepository;
import com.siceb.platform.iam.repository.UserRepository;
import com.siceb.platform.iam.service.JwtTokenService;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mid-session branch context switching via JWT re-issuance (ESC-03 < 3 seconds).
 */
@Service
public class BranchContextService {

    private static final Logger log = LoggerFactory.getLogger(BranchContextService.class);

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final MedicalStaffRepository medicalStaffRepository;
    private final JwtTokenService jwtTokenService;
    private final AuditEventReceiver auditEventReceiver;

    public BranchContextService(
            UserRepository userRepository,
            BranchRepository branchRepository,
            MedicalStaffRepository medicalStaffRepository,
            JwtTokenService jwtTokenService,
            AuditEventReceiver auditEventReceiver) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.medicalStaffRepository = medicalStaffRepository;
        this.jwtTokenService = jwtTokenService;
        this.auditEventReceiver = auditEventReceiver;
    }

    @Transactional
    public BranchSwitchResult switchBranch(UUID userId, UUID targetBranchId) {
        Branch targetBranch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "Branch not found"));

        if (!targetBranch.isActive()) {
            throw new BranchException(ErrorCode.BRANCH_ACCESS_DENIED, "Branch is deactivated");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BranchException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        boolean isAssigned = user.getAssignedBranches().stream()
                .anyMatch(b -> b.getBranchId().equals(targetBranchId));
        if (!isAssigned) {
            throw new BranchException(ErrorCode.BRANCH_ACCESS_DENIED, "User is not assigned to requested branch");
        }

        MedicalStaff medicalStaff = medicalStaffRepository.findByUserUserId(userId).orElse(null);
        String accessToken = jwtTokenService.generateAccessToken(user, targetBranchId, medicalStaff);

        user.setLastActiveBranchId(targetBranchId);
        userRepository.save(user);

        log.info("Branch context switched: user={}, branch={}", user.getUsername(), targetBranch.getName());
        emitAuditEvent(userId, targetBranchId, targetBranch.getName());

        return new BranchSwitchResult(accessToken, targetBranchId, targetBranch.getName());
    }

    public UUID resolveInitialBranch(User user) {
        // Prefer last active branch if still assigned and active
        if (user.getLastActiveBranchId() != null) {
            Branch lastBranch = branchRepository.findById(user.getLastActiveBranchId()).orElse(null);
            if (lastBranch != null && lastBranch.isActive()) {
                boolean stillAssigned = user.getAssignedBranches().stream()
                        .anyMatch(b -> b.getBranchId().equals(user.getLastActiveBranchId()));
                if (stillAssigned) {
                    return user.getLastActiveBranchId();
                }
            }
        }

        // Fallback to first assigned active branch
        return user.getAssignedBranches().stream()
                .filter(Branch::isActive)
                .map(Branch::getBranchId)
                .findFirst()
                .orElseThrow(() -> new BranchException(ErrorCode.BRANCH_CONTEXT_REQUIRED,
                        "User has no authorized active branches"));
    }

    private void emitAuditEvent(UUID userId, UUID branchId, String branchName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "BRANCH_CONTEXT_SWITCH");
        payload.put("branchName", branchName);
        auditEventReceiver.recordSecurityEvent(
                new AuditEventReceiver.SecurityAuditEvent(
                        "BRANCH_CONTEXT_SWITCH", userId, branchId, "Branch", branchId, null, null, payload));
    }

    public record BranchSwitchResult(String accessToken, UUID branchId, String branchName) {}
}
