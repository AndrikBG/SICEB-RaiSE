package com.siceb.platform.iam.security;

import io.jsonwebtoken.Claims;

import java.util.*;

/**
 * Shared extraction of {@link SicebUserPrincipal} from JWT {@link Claims}.
 * Used by both {@link JwtAuthenticationFilter} (HTTP) and
 * {@link com.siceb.platform.realtime.WebSocketSecurityInterceptor} (STOMP).
 */
public final class JwtClaimsPrincipalMapper {

    private JwtClaimsPrincipalMapper() {}

    public static SicebUserPrincipal fromClaims(Claims claims) {
        @SuppressWarnings("unchecked")
        Collection<String> permissions = claims.get("permissions", Collection.class);
        @SuppressWarnings("unchecked")
        Collection<String> branchAssignments = claims.get("branchAssignments", Collection.class);

        return new SicebUserPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("fullName", String.class),
                claims.get("role", String.class),
                claims.get("residencyLevel", String.class),
                claims.get("activeBranchId", String.class) != null
                        ? UUID.fromString(claims.get("activeBranchId", String.class))
                        : null,
                permissions != null ? new HashSet<>(permissions) : Set.of(),
                branchAssignments != null ? new HashSet<>(branchAssignments) : Set.of(),
                claims.get("staffId", String.class) != null
                        ? UUID.fromString(claims.get("staffId", String.class))
                        : null
        );
    }
}
