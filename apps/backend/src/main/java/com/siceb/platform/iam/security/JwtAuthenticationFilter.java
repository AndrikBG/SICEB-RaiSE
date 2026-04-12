package com.siceb.platform.iam.security;

import com.siceb.platform.iam.service.JwtTokenService;
import com.siceb.platform.iam.service.TokenDenyListService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Security Middleware Pipeline — Filter 2: AuthenticationFilter (requeriments3.md §5.4).
 * Validates JWT signature, expiry, and checks JTI against TokenDenyList.
 * On success, populates SecurityContext with user claims.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final TokenDenyListService tokenDenyListService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, TokenDenyListService tokenDenyListService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenDenyListService = tokenDenyListService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtTokenService.parseAccessToken(token);

            // Check token deny list
            String jti = claims.getId();
            if (jti != null && tokenDenyListService.isDenied(jti)) {
                log.warn("Denied token used: jti={}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {"code":"SICEB-2003","message":"Token has been revoked","correlationId":"%s"}
                    """.formatted(UUID.randomUUID()));
                return;
            }

            @SuppressWarnings("unchecked")
            Collection<String> permissions = claims.get("permissions", Collection.class);
            List<SimpleGrantedAuthority> authorities = permissions != null
                    ? permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                    : List.of();

            @SuppressWarnings("unchecked")
            Collection<String> branchAssignments = claims.get("branchAssignments", Collection.class);

            SicebUserPrincipal principal = new SicebUserPrincipal(
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

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            // Don't set authentication — Spring Security will handle 401
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/login") || path.startsWith("/auth/refresh");
    }
}
