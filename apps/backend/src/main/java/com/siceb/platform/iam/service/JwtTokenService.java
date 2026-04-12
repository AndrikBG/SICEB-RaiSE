package com.siceb.platform.iam.service;

import com.siceb.platform.iam.entity.MedicalStaff;
import com.siceb.platform.iam.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT token issuance and validation service.
 * Produces access tokens (15-min TTL) with embedded claims:
     * userId, role, residencyLevel, branchAssignments, activeBranchId, permissions, consentScopes.
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Issue an access token with all required claims per requeriments3.md.
     */
    public String generateAccessToken(User user, UUID activeBranchId, MedicalStaff medicalStaff) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpirationMs);

        Set<String> permissions = user.getRole().getPermissions().stream()
                .map(p -> p.getKey())
                .collect(Collectors.toSet());

        List<String> branchAssignments = user.getAssignedBranches().stream()
                .map(b -> b.getBranchId().toString())
                .toList();

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUserId().toString())
                .claim("username", user.getUsername())
                .claim("fullName", user.getFullName())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getName())
                .claim("roleId", user.getRole().getRoleId().toString())
                .claim("permissions", permissions)
                .claim("branchAssignments", branchAssignments)
                .claim("activeBranchId", activeBranchId != null ? activeBranchId.toString() : null)
                .claim("mustChangePassword", user.isMustChangePassword())
                .claim("consentScopes", List.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey);

        // Add residency level if medical staff
        if (medicalStaff != null) {
            builder.claim("residencyLevel", medicalStaff.getResidencyLevel());
            builder.claim("staffId", medicalStaff.getStaffId().toString());
            builder.claim("canPrescribeControlled", medicalStaff.isCanPrescribeControlled());
        }

        return builder.compact();
    }

    /**
     * Generate an opaque refresh token (raw UUID string).
     * The hash is stored server-side; the raw value is sent to the client.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Parse and validate a JWT access token.
     * Returns the claims if valid, throws on expired/invalid/tampered tokens.
     */
    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the JTI (unique token ID) from a raw token.
     */
    public String extractJti(String token) {
        return parseAccessToken(token).getId();
    }

    /**
     * Extract the user ID from a raw token.
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseAccessToken(token).getSubject());
    }

    /**
     * Check if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseAccessToken(token);
            return claims.getExpiration().toInstant().isBefore(Instant.now());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public long getAccessTokenExpirationMs() { return accessTokenExpirationMs; }
    public long getRefreshTokenExpirationMs() { return refreshTokenExpirationMs; }
}
