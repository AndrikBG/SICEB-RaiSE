package com.siceb.platform.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 200)
    private String action;

    @Column(name = "target_entity", length = 200)
    private String targetEntity;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "entry_hash")
    private String entryHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntry() {}

    @SuppressWarnings("java:S107")
    public AuditLogEntry(
            UUID entryId,
            UUID branchId,
            UUID userId,
            String action,
            String targetEntity,
            UUID targetId,
            String ipAddress,
            String userAgent,
            String payload
    ) {
        this.entryId = entryId;
        this.branchId = branchId;
        this.userId = userId;
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.payload = payload;
    }

    public UUID getEntryId() { return entryId; }
    public UUID getBranchId() { return branchId; }
    public UUID getUserId() { return userId; }
    public String getAction() { return action; }
    public String getTargetEntity() { return targetEntity; }
    public UUID getTargetId() { return targetId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getPayload() { return payload; }
    public String getPreviousHash() { return previousHash; }
    public String getEntryHash() { return entryHash; }
    public Instant getCreatedAt() { return createdAt; }
}

