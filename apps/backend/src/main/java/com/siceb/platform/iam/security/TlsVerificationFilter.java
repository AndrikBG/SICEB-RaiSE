package com.siceb.platform.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Security Middleware Pipeline — Filter 1: TlsVerifier (requeriments3.md §5.4).
 * Defense-in-depth: rejects non-HTTPS requests when running behind a TLS-terminating
 * load balancer that sets {@code X-Forwarded-Proto}.
 * <p>
 * Disabled in local development ({@code tls.enforce=false}) since Docker Compose
 * does not use TLS.
 */
@Component
public class TlsVerificationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TlsVerificationFilter.class);

    @Value("${tls.enforce:false}")
    private boolean enforce;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (enforce) {
            String proto = request.getHeader("X-Forwarded-Proto");
            boolean secure = request.isSecure()
                    || "https".equalsIgnoreCase(proto);

            if (!secure) {
                log.warn("TLS required but request is not secure: method={}, uri={}, proto={}",
                        request.getMethod(), request.getRequestURI(), proto);
                response.setStatus(421);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {"code":"SICEB-0003","message":"HTTPS required","correlationId":"%s"}
                    """.formatted(UUID.randomUUID()));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
