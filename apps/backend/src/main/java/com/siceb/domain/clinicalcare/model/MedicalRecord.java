package com.siceb.domain.clinicalcare.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Exactly one medical record per patient (US-026, CRN-02).
 * Immutable once created — all clinical data appended via ClinicalEvent.
 * Permanent retention per NOM-004-SSA3-2012 (CRN-01).
 */
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "patient_id", nullable = false, unique = true)
    private UUID patientId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_staff_id", nullable = false)
    private UUID createdByStaffId;

    protected MedicalRecord() {}

    public MedicalRecord(UUID recordId, UUID patientId, UUID branchId, UUID createdByStaffId) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.branchId = branchId;
        this.createdByStaffId = createdByStaffId;
        this.createdAt = Instant.now();
    }

    public UUID getRecordId() { return recordId; }
    public UUID getPatientId() { return patientId; }
    public UUID getBranchId() { return branchId; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedByStaffId() { return createdByStaffId; }
}
