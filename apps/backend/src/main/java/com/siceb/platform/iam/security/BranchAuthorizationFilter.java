package com.siceb.platform.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Security Middleware Pipeline — Filter 3: AuthorizationFilter (requeriments3.md §5.4).
 * Validates that the authenticated user is assigned to their active branch (SEC-02).
 * Runs after JwtAuthenticationFilter has populated the SecurityContext.
 * <p>
 * Fine-grained permission + residency checks are handled by {@code @PreAuthorize}
 * annotations on controller methods via {@link AuthorizationService}.
 */
@Component
public class BranchAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BranchAuthorizationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof SicebUserPrincipal principal) {
            UUID activeBranch = principal.activeBranchId();
            if (activeBranch != null && !principal.isAssignedToBranch(activeBranch)) {
                log.warn("Branch access denied: user={}, activeBranch={}, assigned={}",
                        principal.userId(), activeBranch, principal.branchAssignments());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {"code":"SICEB-5002","message":"User not assigned to the active branch","correlationId":"%s"}
                    """.formatted(UUID.randomUUID()));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/actuator/");
    }
}
