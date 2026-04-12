package com.siceb.config;

import com.siceb.platform.iam.security.BranchAuthorizationFilter;
import com.siceb.platform.iam.security.JwtAuthenticationFilter;
import com.siceb.platform.iam.security.TlsVerificationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration implementing the 6-filter middleware pipeline
 * per requeriments3.md §5.4:
 * <ol>
 *   <li>TlsVerifier — defense-in-depth via X-Forwarded-Proto check</li>
 *   <li>AuthenticationFilter — JwtAuthenticationFilter (JWT + deny-list)</li>
 *   <li>AuthorizationFilter — BranchAuthorizationFilter (branch assignment)
 *       + @PreAuthorize (permission + residency via AuthorizationService)</li>
 *   <li>TenantContextInjector — TenantFilter (JWT claims → TenantContext + StaffContext)</li>
 *   <li>AuditInterceptor — access event per authenticated request (WebMvcConfig)</li>
 *   <li>ErrorSanitizer — GlobalExceptionHandler</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    private final TlsVerificationFilter tlsVerificationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BranchAuthorizationFilter branchAuthorizationFilter;

    public SecurityConfig(TlsVerificationFilter tlsVerificationFilter,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          BranchAuthorizationFilter branchAuthorizationFilter) {
        this.tlsVerificationFilter = tlsVerificationFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.branchAuthorizationFilter = branchAuthorizationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/login",
                    "/auth/refresh",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/api-docs/**",
                    "/docs/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Pipeline order: TLS → JWT auth → branch authorization
            .addFilterBefore(tlsVerificationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, TlsVerificationFilter.class)
            .addFilterAfter(branchAuthorizationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // Required for HttpOnly cookie (IC-04)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
