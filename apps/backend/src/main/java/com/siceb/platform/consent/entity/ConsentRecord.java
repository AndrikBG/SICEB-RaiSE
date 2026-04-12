package com.siceb.platform.consent.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @Column(name = "consent_id")
    private UUID consentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "consent_type", nullable = false, length = 100)
    private String consentType;

    @Column(name = "purpose", nullable = false, length = 500)
    private String purpose;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected ConsentRecord() {}

    public ConsentRecord(UUID patientId, UUID branchId, String consentType,
                         String purpose, UUID grantedBy) {
        this.consentId = UUID.randomUUID();
        this.patientId = patientId;
        this.branchId = branchId;
        this.consentType = consentType;
        this.purpose = purpose;
        this.grantedAt = Instant.now();
        this.grantedBy = grantedBy;
    }

    public void revoke(UUID revokedByUserId) {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Consent already revoked");
        }
        this.revokedAt = Instant.now();
        this.revokedBy = revokedByUserId;
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public UUID getConsentId() { return consentId; }
    public UUID getPatientId() { return patientId; }
    public UUID getBranchId() { return branchId; }
    public String getConsentType() { return consentType; }
    public String getPurpose() { return purpose; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getGrantedBy() { return grantedBy; }
    public UUID getRevokedBy() { return revokedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
