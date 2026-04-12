package com.siceb.platform.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medical_staff")
public class MedicalStaff {

    @Id
    @Column(name = "staff_id")
    private UUID staffId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "specialty", nullable = false, length = 200)
    private String specialty;

    @Column(name = "residency_level", length = 10)
    private String residencyLevel;

    @Column(name = "can_prescribe_controlled", nullable = false)
    private boolean canPrescribeControlled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_staff_id")
    private MedicalStaff supervisor;

    @Column(name = "professional_license", length = 100)
    private String professionalLicense;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicalStaff() {}

    public MedicalStaff(User user, String specialty, String residencyLevel, boolean canPrescribeControlled) {
        this.staffId = UUID.randomUUID();
        this.user = user;
        this.specialty = specialty;
        this.residencyLevel = residencyLevel;
        this.canPrescribeControlled = canPrescribeControlled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void ensureId() {
        if (this.staffId == null) this.staffId = UUID.randomUUID();
    }

    // Getters
    public UUID getStaffId() { return staffId; }
    public User getUser() { return user; }
    public String getSpecialty() { return specialty; }
    public String getResidencyLevel() { return residencyLevel; }
    public boolean isCanPrescribeControlled() { return canPrescribeControlled; }
    public MedicalStaff getSupervisor() { return supervisor; }
    public String getProfessionalLicense() { return professionalLicense; }

    // Setters
    public void setSpecialty(String specialty) { this.specialty = specialty; this.updatedAt = Instant.now(); }
    public void setResidencyLevel(String residencyLevel) { this.residencyLevel = residencyLevel; this.updatedAt = Instant.now(); }
    public void setCanPrescribeControlled(boolean can) { this.canPrescribeControlled = can; this.updatedAt = Instant.now(); }
    public void setSupervisor(MedicalStaff supervisor) { this.supervisor = supervisor; this.updatedAt = Instant.now(); }
    public void setProfessionalLicense(String license) { this.professionalLicense = license; this.updatedAt = Instant.now(); }

    public boolean isResident() {
        return residencyLevel != null;
    }

    public boolean requiresSupervision() {
        return "R1".equals(residencyLevel) || "R2".equals(residencyLevel);
    }
}
