package com.siceb.platform.branch;

import java.io.IOException;
import java.util.UUID;

import com.siceb.platform.iam.StaffContext;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Security Middleware Pipeline — Filter 4: TenantContextInjector (requeriments3.md §5.4).
 * Sets {@link TenantContext} and {@link StaffContext} from JWT claims (activeBranchId, staffId).
 * Falls back to X-Branch-Id / X-Staff-Id headers when no authenticated principal exists
 * (dev/test convenience).
 */
@Component
@Order(1)
public class TenantFilter implements Filter {

    static final String BRANCH_HEADER = "X-Branch-Id";
    static final String STAFF_HEADER = "X-Staff-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof SicebUserPrincipal principal) {
                if (principal.activeBranchId() != null) {
                    TenantContext.set(principal.activeBranchId());
                }
                if (principal.userId() != null) {
                    StaffContext.set(principal.userId());
                }
            } else if (request instanceof HttpServletRequest httpReq) {
                String branchHeader = httpReq.getHeader(BRANCH_HEADER);
                if (branchHeader != null && !branchHeader.isBlank()) {
                    TenantContext.set(UUID.fromString(branchHeader.trim()));
                }
                String staffHeader = httpReq.getHeader(STAFF_HEADER);
                if (staffHeader != null && !staffHeader.isBlank()) {
                    StaffContext.set(UUID.fromString(staffHeader.trim()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            StaffContext.clear();
        }
    }
}
