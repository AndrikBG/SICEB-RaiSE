package com.siceb.platform.audit;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.audit.service.AuditEventReceiver.SecurityAuditEvent;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Security Middleware Pipeline — Filter 5: AuditInterceptor (requeriments3.md §5.4).
 * Records an access event for every authenticated API request.
 * Runs as a Spring MVC interceptor (after filter chain) to capture the resolved handler.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditEventReceiver auditEventReceiver;

    public AuditInterceptor(AuditEventReceiver auditEventReceiver) {
        this.auditEventReceiver = auditEventReceiver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SicebUserPrincipal principal) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String ip = extractIp(request);
            String ua = request.getHeader("User-Agent");

            try {
                auditEventReceiver.recordAccessEventAsync(SecurityAuditEvent.accessEvent(
                        principal.userId(),
                        principal.activeBranchId(),
                        method, path,
                        null, null,
                        ip, ua
                ));
            } catch (Exception ignored) {
                // Access audit must not block the request
            }
        }
        return true;
    }

    private String extractIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
