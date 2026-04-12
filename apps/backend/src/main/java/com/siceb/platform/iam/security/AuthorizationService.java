package com.siceb.platform.iam.security;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.audit.service.AuditEventReceiver.SecurityAuditEvent;
import com.siceb.platform.iam.service.IamException;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Three-dimensional authorization middleware per requeriments3.md §5.2:
 * <ol>
 *   <li>Role permission — does the user's role grant the required permission?</li>
 *   <li>Branch assignment — is the user assigned to the active branch?</li>
 *   <li>Residency level — does {@link ResidencyLevelPolicy} allow the action?</li>
 * </ol>
 * All denials are audit-logged (SEC-01).
 */
@Service("auth")
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final ResidencyLevelPolicy residencyLevelPolicy;
    private final AuditEventReceiver auditEventReceiver;

    public AuthorizationService(ResidencyLevelPolicy residencyLevelPolicy,
                                AuditEventReceiver auditEventReceiver) {
        this.residencyLevelPolicy = residencyLevelPolicy;
        this.auditEventReceiver = auditEventReceiver;
    }

    /**
     * Full three-dimensional check using an explicit request object.
     *
     * @return result indicating authorization and supervision status
     * @throws IamException if any dimension fails
     */
    public AuthorizationResult authorize(AuthorizationRequest request) {
        // Dimension 1: role permission
        if (!request.principal().hasPermission(request.requiredPermission())) {
            String reason = "Permission '%s' not granted to role '%s'"
                    .formatted(request.requiredPermission(), request.principal().role());
            auditDenial(request.principal(), request.requiredPermission(), reason,
                    request.ipAddress(), request.userAgent());
            throw new IamException(ErrorCode.FORBIDDEN, reason);
        }

        // Dimension 2: branch assignment
        UUID activeBranch = request.principal().activeBranchId();
        if (activeBranch != null && !request.principal().isAssignedToBranch(activeBranch)) {
            String reason = "User not assigned to branch %s".formatted(activeBranch);
            auditDenial(request.principal(), request.requiredPermission(), reason,
                    request.ipAddress(), request.userAgent());
            throw new IamException(ErrorCode.BRANCH_ACCESS_DENIED, reason);
        }

        // Dimension 3: residency level policy
        ResidencyLevelPolicy.EvaluationResult residency = residencyLevelPolicy.evaluate(
                request.principal().residencyLevel(),
                request.requiredPermission(),
                request.requiresResidencyCheck());

        if (!residency.permitted()) {
            auditDenial(request.principal(), request.requiredPermission(), residency.reason(),
                    request.ipAddress(), request.userAgent());
            throw new IamException(ErrorCode.RESIDENCY_RESTRICTED, residency.reason());
        }

        return new AuthorizationResult(true, residency.supervisionRequired());
    }

    /**
     * SpEL-callable check for {@code @PreAuthorize("@auth.check('permission:key')")}.
     * Resolves the current principal from SecurityContext and applies all three dimensions.
     *
     * @return true if authorized (throws on denial, so always true on return)
     */
    public boolean check(String requiredPermission) {
        return check(requiredPermission, false);
    }

    /**
     * SpEL-callable check with explicit residency flag.
     * Use for permissions that have {@code requires_residency_check = true} in DB.
     */
    public boolean check(String requiredPermission, boolean requiresResidencyCheck) {
        SicebUserPrincipal principal = currentPrincipal();

        if (!principal.hasPermission(requiredPermission)) {
            String reason = "Permission '%s' not granted to role '%s'"
                    .formatted(requiredPermission, principal.role());
            auditDenial(principal, requiredPermission, reason, null, null);
            throw new IamException(ErrorCode.FORBIDDEN, reason);
        }

        UUID activeBranch = principal.activeBranchId();
        if (activeBranch != null && !principal.isAssignedToBranch(activeBranch)) {
            String reason = "User not assigned to branch %s".formatted(activeBranch);
            auditDenial(principal, requiredPermission, reason, null, null);
            throw new IamException(ErrorCode.BRANCH_ACCESS_DENIED, reason);
        }

        if (requiresResidencyCheck) {
            ResidencyLevelPolicy.EvaluationResult r = residencyLevelPolicy.evaluate(
                    principal.residencyLevel(), requiredPermission, true);
            if (!r.permitted()) {
                auditDenial(principal, requiredPermission, r.reason(), null, null);
                throw new IamException(ErrorCode.RESIDENCY_RESTRICTED, r.reason());
            }
        }

        return true;
    }

    /**
     * Verify that the current principal is assigned to the given branch.
     */
    public void requireBranchAccess(UUID branchId) {
        SicebUserPrincipal principal = currentPrincipal();
        if (!principal.isAssignedToBranch(branchId)) {
            throw new IamException(ErrorCode.BRANCH_ACCESS_DENIED,
                    "User not assigned to branch %s".formatted(branchId));
        }
    }

    public SicebUserPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SicebUserPrincipal p)) {
            throw new IamException(ErrorCode.UNAUTHORIZED, "No authenticated principal");
        }
        return p;
    }

    private void auditDenial(SicebUserPrincipal principal, String permission,
                             String reason, String ip, String ua) {
        log.warn("Authorization denied: user={}, permission={}, branch={}, reason={}",
                principal.userId(), permission, principal.activeBranchId(), reason);
        try {
            auditEventReceiver.recordSecurityEvent(SecurityAuditEvent.permissionDenied(
                    principal.userId(), principal.activeBranchId(), permission, reason, ip, ua));
        } catch (Exception e) {
            log.error("Failed to record authorization denial audit event", e);
        }
    }

    // ---- Request / Result records ----

    public record AuthorizationRequest(
            SicebUserPrincipal principal,
            String requiredPermission,
            boolean requiresResidencyCheck,
            String ipAddress,
            String userAgent
    ) {}

    public record AuthorizationResult(boolean authorized, boolean supervisionRequired) {}
}
