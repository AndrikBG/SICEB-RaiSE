package com.siceb.platform.iam.security;

import java.util.Set;
import java.util.UUID;

/**
 * Authenticated user principal extracted from JWT claims.
 * Available in SecurityContext after JwtAuthenticationFilter processes a valid token.
 */
public record SicebUserPrincipal(
        UUID userId,
        String username,
        String fullName,
        String role,
        String residencyLevel,
        UUID activeBranchId,
        Set<String> permissions,
        Set<String> branchAssignments,
        UUID staffId
) {

    public boolean hasPermission(String permissionKey) {
        return permissions != null && permissions.contains(permissionKey);
    }

    public boolean isAssignedToBranch(UUID branchId) {
        return branchAssignments != null && branchId != null
                && branchAssignments.contains(branchId.toString());
    }

    public boolean isResident() {
        return residencyLevel != null;
    }

    public boolean isAttendingPhysician() {
        return "Médico Adscrito".equals(role);
    }
}
