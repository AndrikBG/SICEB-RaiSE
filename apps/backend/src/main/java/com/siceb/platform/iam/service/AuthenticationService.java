package com.siceb.platform.iam.service;

import com.siceb.platform.branch.service.BranchContextService;
import com.siceb.platform.iam.entity.MedicalStaff;
import com.siceb.platform.iam.entity.RefreshToken;
import com.siceb.platform.iam.entity.User;
import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.iam.repository.MedicalStaffRepository;
import com.siceb.platform.iam.repository.RefreshTokenRepository;
import com.siceb.platform.iam.repository.UserRepository;
import com.siceb.shared.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * AuthenticationService per requeriments3.md §5.2:
 * - Login: credential validation → JWT + refresh token
 * - Refresh: token rotation → new JWT
 * - Logout: revocation of refresh + deny access token
 * - Account lockout after 5 failed attempts (15 min)
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15L * 60L * 1000L; // 15 minutes

    private final UserRepository userRepository;
    private final MedicalStaffRepository medicalStaffRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final TokenDenyListService tokenDenyListService;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventReceiver auditEventReceiver;
    private final BranchContextService branchContextService;

    public AuthenticationService(
            UserRepository userRepository,
            MedicalStaffRepository medicalStaffRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService,
            TokenDenyListService tokenDenyListService,
            PasswordEncoder passwordEncoder,
            AuditEventReceiver auditEventReceiver,
            BranchContextService branchContextService
    ) {
        this.userRepository = userRepository;
        this.medicalStaffRepository = medicalStaffRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.tokenDenyListService = tokenDenyListService;
        this.passwordEncoder = passwordEncoder;
        this.auditEventReceiver = auditEventReceiver;
        this.branchContextService = branchContextService;
    }

    /**
     * Authenticate with username + password.
     * Returns LoginResult with JWT, refresh token, and user info.
     */
    @Transactional
    public LoginResult login(String username, String password, String ipAddress, String userAgent) {
        User user;
        try {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password"));
        } catch (AuthenticationException ex) {
            auditEventReceiver.recordSecurityEvent(
                    AuditEventReceiver.SecurityAuditEvent.loginFailure(username, ipAddress, userAgent, ex.getErrorCode().name())
            );
            throw ex;
        }

        // Check if account is locked
        if (user.isLocked()) {
            auditEventReceiver.recordSecurityEvent(
                    AuditEventReceiver.SecurityAuditEvent.loginFailure(username, ipAddress, userAgent, ErrorCode.UNAUTHORIZED.name())
            );
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED,
                    "Account locked for 15 minutes due to multiple failed attempts");
        }

        // Check if account is active
        if (!user.isActive()) {
            auditEventReceiver.recordSecurityEvent(
                    AuditEventReceiver.SecurityAuditEvent.loginFailure(username, ipAddress, userAgent, ErrorCode.UNAUTHORIZED.name())
            );
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED,
                    "Your account is inactive. Contact the administrator");
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockUntil(Instant.now().plusMillis(LOCK_DURATION_MS));
                log.warn("Account locked: user={}, attempts={}", username, user.getFailedLoginAttempts());
            }
            userRepository.save(user);
            auditEventReceiver.recordSecurityEvent(
                    AuditEventReceiver.SecurityAuditEvent.loginFailure(username, ipAddress, userAgent, ErrorCode.INVALID_CREDENTIALS.name())
            );
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }

        // Successful login — reset failed attempts
        user.resetFailedAttempts();
        userRepository.save(user);

        // Load medical staff info if applicable
        MedicalStaff medicalStaff = medicalStaffRepository.findByUserUserId(user.getUserId()).orElse(null);

        // Resolve active branch: last active → first assigned → error
        UUID activeBranchId;
        try {
            activeBranchId = branchContextService.resolveInitialBranch(user);
        } catch (Exception e) {
            // Fallback to primary branch if resolution fails (e.g., no assignments yet)
            activeBranchId = user.getBranchId();
        }

        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(user, activeBranchId, medicalStaff);
        String rawRefreshToken = jwtTokenService.generateRefreshToken();

        // Store refresh token hash
        String refreshTokenHash = hashToken(rawRefreshToken);
        Instant refreshExpiry = Instant.now().plusMillis(jwtTokenService.getRefreshTokenExpirationMs());
        RefreshToken refreshToken = new RefreshToken(user.getUserId(), refreshTokenHash, refreshExpiry, ipAddress, userAgent);
        refreshTokenRepository.save(refreshToken);

        log.info("Login successful: user={}, role={}", username, user.getRole().getName());
        auditEventReceiver.recordSecurityEvent(
                AuditEventReceiver.SecurityAuditEvent.loginSuccess(user.getUserId(), activeBranchId, username, ipAddress, userAgent)
        );

        return new LoginResult(
                accessToken,
                rawRefreshToken,
                user,
                medicalStaff,
                activeBranchId,
                user.isMustChangePassword()
        );
    }

    /**
     * Refresh an access token using a valid refresh token.
     */
    @Transactional
    public RefreshResult refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (!storedToken.isActive()) {
            // Token reuse detection — revoke all tokens for this user
            if (storedToken.isRevoked()) {
                refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
                log.warn("Refresh token reuse detected for user={}", storedToken.getUserId());
            }
            throw new AuthenticationException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired or revoked");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new AuthenticationException(ErrorCode.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED, "Account is inactive");
        }

        MedicalStaff medicalStaff = medicalStaffRepository.findByUserUserId(user.getUserId()).orElse(null);
        UUID activeBranchId = user.getLastActiveBranchId() != null
                ? user.getLastActiveBranchId()
                : user.getBranchId();

        // Generate new tokens (rotation)
        String newAccessToken = jwtTokenService.generateAccessToken(user, activeBranchId, medicalStaff);
        String newRawRefreshToken = jwtTokenService.generateRefreshToken();
        String newRefreshHash = hashToken(newRawRefreshToken);
        Instant newExpiry = Instant.now().plusMillis(jwtTokenService.getRefreshTokenExpirationMs());

        RefreshToken newRefreshToken = new RefreshToken(user.getUserId(), newRefreshHash, newExpiry, ipAddress, userAgent);
        refreshTokenRepository.save(newRefreshToken);

        // Revoke old token with forward link
        storedToken.replaceWith(newRefreshToken.getTokenId());
        refreshTokenRepository.save(storedToken);

        log.debug("Token refreshed for user={}", user.getUsername());
        auditEventReceiver.recordSecurityEvent(
                AuditEventReceiver.SecurityAuditEvent.refresh(user.getUserId(), activeBranchId, ipAddress, userAgent)
        );

        return new RefreshResult(newAccessToken, newRawRefreshToken);
    }

    /**
     * Logout — revoke refresh token and deny access token.
     */
    @Transactional
    public void logout(String accessToken, String rawRefreshToken, String ipAddress, String userAgent) {
        UUID userIdForAudit = null;
        UUID branchIdForAudit = null;
        // Deny the access token
        try {
            Claims claims = jwtTokenService.parseAccessToken(accessToken);
            String jti = claims.getId();
            UUID userId = UUID.fromString(claims.getSubject());
            userIdForAudit = userId;
            String activeBranchId = claims.get("activeBranchId", String.class);
            if (activeBranchId != null) branchIdForAudit = UUID.fromString(activeBranchId);
            Instant expiry = claims.getExpiration().toInstant();
            tokenDenyListService.denyToken(jti, userId, expiry, "logout");
        } catch (JwtException e) {
            log.debug("Could not parse access token during logout — likely already expired");
        }

        // Revoke refresh token
        if (rawRefreshToken != null) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.revoke();
                refreshTokenRepository.save(rt);
            });
        }

        if (userIdForAudit != null) {
            auditEventReceiver.recordSecurityEvent(
                    AuditEventReceiver.SecurityAuditEvent.logout(userIdForAudit, branchIdForAudit, ipAddress, userAgent)
            );
        }
    }

    /**
     * Change password (for forced password change on first login).
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }

        validatePasswordPolicy(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Password changed for user={}", user.getUsername());
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new AuthenticationException(ErrorCode.VALIDATION_FAILED,
                    "The password must be at least 8 characters long");
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new AuthenticationException(ErrorCode.VALIDATION_FAILED,
                    "The password must contain at least one letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new AuthenticationException(ErrorCode.VALIDATION_FAILED,
                    "The password must contain at least one number");
        }
    }

    /**
     * Extract user ID from a raw access token.
     */
    public UUID extractUserId(String token) {
        return jwtTokenService.extractUserId(token);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ---- Result records ----

    public record LoginResult(
            String accessToken,
            String refreshToken,
            User user,
            MedicalStaff medicalStaff,
            UUID activeBranchId,
            boolean mustChangePassword
    ) {}

    public record RefreshResult(
            String accessToken,
            String refreshToken
    ) {}

    // ---- Auth exception ----

    public static class AuthenticationException extends RuntimeException {
        private final ErrorCode errorCode;

        public AuthenticationException(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public ErrorCode getErrorCode() { return errorCode; }
    }
}
