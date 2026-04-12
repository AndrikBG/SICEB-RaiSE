package com.siceb.domain.clinicalcare.readmodel;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Denormalized, indexed projection for fast patient lookup (PER-03).
 * Targets sub-1s over 50,000+ records with pg_trgm + B-tree indexes.
 */
@Entity
@Table(name = "patient_search_view")
public class PatientSearchEntry {

    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "patient_type", nullable = false, length = 20)
    private String patientType;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "credential_number", length = 50)
    private String credentialNumber;

    @Column(name = "profile_status", nullable = false, length = 20)
    private String profileStatus;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "last_visit_date")
    private Instant lastVisitDate;

    @Column(name = "consultation_count", nullable = false)
    private int consultationCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PatientSearchEntry() {}

    public PatientSearchEntry(UUID patientId, String fullName, LocalDate dateOfBirth,
                               String patientType, String gender, String phone,
                               String curp, String credentialNumber,
                               String profileStatus, UUID branchId, UUID recordId) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.patientType = patientType;
        this.gender = gender;
        this.phone = phone;
        this.curp = curp;
        this.credentialNumber = credentialNumber;
        this.profileStatus = profileStatus;
        this.branchId = branchId;
        this.recordId = recordId;
        this.consultationCount = 0;
        this.createdAt = Instant.now();
    }

    public void updateLastVisit(Instant visitDate) {
        this.lastVisitDate = visitDate;
        this.consultationCount++;
    }

    public UUID getPatientId() { return patientId; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPatientType() { return patientType; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getCurp() { return curp; }
    public String getCredentialNumber() { return credentialNumber; }
    public String getProfileStatus() { return profileStatus; }
    public UUID getBranchId() { return branchId; }
    public UUID getRecordId() { return recordId; }
    public Instant getLastVisitDate() { return lastVisitDate; }
    public int getConsultationCount() { return consultationCount; }
    public Instant getCreatedAt() { return createdAt; }
}
