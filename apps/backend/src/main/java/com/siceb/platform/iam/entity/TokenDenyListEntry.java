package com.siceb.platform.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_deny_list")
public class TokenDenyListEntry {

    @Id
    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "jti", nullable = false, length = 255, unique = true)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TokenDenyListEntry() {}

    public TokenDenyListEntry(String jti, UUID userId, Instant expiresAt, String reason) {
        this.entryId = UUID.randomUUID();
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getEntryId() { return entryId; }
    public String getJti() { return jti; }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
