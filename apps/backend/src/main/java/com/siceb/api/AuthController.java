package com.siceb.api;

import com.siceb.platform.iam.service.AuthenticationService;
import com.siceb.platform.iam.service.AuthenticationService.LoginResult;
import com.siceb.platform.iam.service.AuthenticationService.RefreshResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "siceb_refresh";
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final AuthenticationService authenticationService;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * POST /auth/login — Authenticate with username + password.
     * Returns JWT access token in response body and refresh token as HttpOnly cookie (IC-04).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ip = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader(USER_AGENT_HEADER);

        LoginResult result = authenticationService.login(request.username(), request.password(), ip, userAgent);

        // Set refresh token as HttpOnly cookie per IC-04
        setRefreshCookie(httpResponse, result.refreshToken());

        // Build branch list
        List<BranchInfo> branches = result.user().getAssignedBranches().stream()
                .map(b -> new BranchInfo(b.getBranchId().toString(), b.getName()))
                .toList();

        // Build user info
        UserInfo userInfo = new UserInfo(
                result.user().getUserId().toString(),
                result.user().getUsername(),
                result.user().getFullName(),
                result.user().getEmail(),
                result.user().getRole().getName(),
                result.user().getRole().getPermissions().stream().map(p -> p.getKey()).collect(Collectors.toSet()),
                result.medicalStaff() != null ? result.medicalStaff().getResidencyLevel() : null,
                result.medicalStaff() != null ? result.medicalStaff().getStaffId().toString() : null,
                result.activeBranchId().toString(),
                branches
        );

        return ResponseEntity.ok(new LoginResponse(
                result.accessToken(),
                userInfo,
                result.mustChangePassword()
        ));
    }

    /**
     * POST /auth/refresh — Silent refresh via HttpOnly cookie.
     * Browser automatically attaches the cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String rawRefreshToken = extractRefreshCookie(httpRequest);
        if (rawRefreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        String ip = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader(USER_AGENT_HEADER);

        RefreshResult result = authenticationService.refresh(rawRefreshToken, ip, userAgent);

        // Rotate cookie
        setRefreshCookie(httpResponse, result.refreshToken());

        return ResponseEntity.ok(new RefreshResponse(result.accessToken()));
    }

    /**
     * POST /auth/logout — Revoke tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String rawRefreshToken = extractRefreshCookie(httpRequest);
        String ip = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader(USER_AGENT_HEADER);

        if (accessToken != null || rawRefreshToken != null) {
            authenticationService.logout(accessToken, rawRefreshToken, ip, userAgent);
        }

        // Clear refresh cookie
        clearRefreshCookie(httpResponse);

        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/change-password — Change password (forced on first login per US-002).
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        // Extract userId from token
        String token = authHeader.substring(7);
        java.util.UUID userId = authenticationService.extractUserId(token);

        authenticationService.changePassword(userId, request.currentPassword(), request.newPassword());

        return ResponseEntity.noContent().build();
    }

    // ---- Cookie helpers (IC-04) ----

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/auth/");
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/auth/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ---- DTOs ----

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginResponse(
            String accessToken,
            UserInfo user,
            boolean mustChangePassword
    ) {}

    public record RefreshResponse(
            String accessToken
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {}

    public record UserInfo(
            String id,
            String username,
            String fullName,
            String email,
            String role,
            Set<String> permissions,
            String residencyLevel,
            String staffId,
            String activeBranchId,
            List<BranchInfo> branches
    ) {}

    public record BranchInfo(
            String id,
            String name
    ) {}
}
