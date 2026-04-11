package com.siceb.domain.clinicalcare.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Patient aggregate root — globally unique via UUID PK (CRN-37).
 * Guardian fields mandatory for minors under 18 (US-023).
 * Classification determines automatic discount (US-020).
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "paternal_surname", nullable = false, length = 100)
    private String paternalSurname;

    @Column(name = "maternal_surname", length = 100)
    private String maternalSurname;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "curp", length = 18)
    private String curp;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_type", nullable = false, length = 20)
    private PatientType patientType;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "credential_number", length = 50)
    private String credentialNumber;

    @Column(name = "guardian_name", length = 200)
    private String guardianName;

    @Column(name = "guardian_relationship", length = 50)
    private String guardianRelationship;

    @Column(name = "guardian_phone", length = 15)
    private String guardianPhone;

    @Column(name = "guardian_id_confirmed", nullable = false)
    private boolean guardianIdConfirmed;

    @Column(name = "data_consent_given", nullable = false)
    private boolean dataConsentGiven;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 20)
    private ProfileStatus profileStatus;

    @Column(name = "special_case", nullable = false)
    private boolean specialCase;

    @Column(name = "special_case_notes")
    private String specialCaseNotes;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_staff_id", nullable = false)
    private UUID createdByStaffId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Patient() {}

    private Patient(Builder b) {
        this.patientId = b.patientId;
        this.firstName = b.firstName;
        this.paternalSurname = b.paternalSurname;
        this.maternalSurname = b.maternalSurname;
        this.dateOfBirth = b.dateOfBirth;
        this.gender = b.gender;
        this.phone = b.phone;
        this.curp = b.curp;
        this.patientType = b.patientType;
        this.discountPercentage = b.patientType.discountPercentage();
        this.credentialNumber = b.credentialNumber;
        this.guardianName = b.guardianName;
        this.guardianRelationship = b.guardianRelationship;
        this.guardianPhone = b.guardianPhone;
        this.guardianIdConfirmed = b.guardianIdConfirmed;
        this.dataConsentGiven = b.dataConsentGiven;
        this.profileStatus = resolveProfileStatus(b);
        this.specialCase = b.specialCase;
        this.specialCaseNotes = b.specialCaseNotes;
        this.branchId = b.branchId;
        this.createdAt = Instant.now();
        this.createdByStaffId = b.createdByStaffId;
        this.updatedAt = this.createdAt;
    }

    public String fullName() {
        if (maternalSurname != null && !maternalSurname.isBlank()) {
            return firstName + " " + paternalSurname + " " + maternalSurname;
        }
        return firstName + " " + paternalSurname;
    }

    public int ageInYears() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isMinor() {
        return ageInYears() < 18;
    }

    public void updateType(PatientType newType, String credentialNumber) {
        this.patientType = newType;
        this.discountPercentage = newType.discountPercentage();
        this.credentialNumber = credentialNumber;
        this.updatedAt = Instant.now();
    }

    private static ProfileStatus resolveProfileStatus(Builder b) {
        if (b.curp == null || b.curp.isBlank()) return ProfileStatus.INCOMPLETE;
        return ProfileStatus.COMPLETE;
    }

    // Getters
    public UUID getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getPaternalSurname() { return paternalSurname; }
    public String getMaternalSurname() { return maternalSurname; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getCurp() { return curp; }
    public PatientType getPatientType() { return patientType; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public String getCredentialNumber() { return credentialNumber; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianRelationship() { return guardianRelationship; }
    public String getGuardianPhone() { return guardianPhone; }
    public boolean isGuardianIdConfirmed() { return guardianIdConfirmed; }
    public boolean isDataConsentGiven() { return dataConsentGiven; }
    public ProfileStatus getProfileStatus() { return profileStatus; }
    public boolean isSpecialCase() { return specialCase; }
    public String getSpecialCaseNotes() { return specialCaseNotes; }
    public UUID getBranchId() { return branchId; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedByStaffId() { return createdByStaffId; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID patientId;
        private String firstName;
        private String paternalSurname;
        private String maternalSurname;
        private LocalDate dateOfBirth;
        private Gender gender;
        private String phone;
        private String curp;
        private PatientType patientType = PatientType.EXTERNAL;
        private String credentialNumber;
        private String guardianName;
        private String guardianRelationship;
        private String guardianPhone;
        private boolean guardianIdConfirmed;
        private boolean dataConsentGiven;
        private boolean specialCase;
        private String specialCaseNotes;
        private UUID branchId;
        private UUID createdByStaffId;

        public Builder patientId(UUID v) { this.patientId = v; return this; }
        public Builder firstName(String v) { this.firstName = v; return this; }
        public Builder paternalSurname(String v) { this.paternalSurname = v; return this; }
        public Builder maternalSurname(String v) { this.maternalSurname = v; return this; }
        public Builder dateOfBirth(LocalDate v) { this.dateOfBirth = v; return this; }
        public Builder gender(Gender v) { this.gender = v; return this; }
        public Builder phone(String v) { this.phone = v; return this; }
        public Builder curp(String v) { this.curp = v; return this; }
        public Builder patientType(PatientType v) { this.patientType = v; return this; }
        public Builder credentialNumber(String v) { this.credentialNumber = v; return this; }
        public Builder guardianName(String v) { this.guardianName = v; return this; }
        public Builder guardianRelationship(String v) { this.guardianRelationship = v; return this; }
        public Builder guardianPhone(String v) { this.guardianPhone = v; return this; }
        public Builder guardianIdConfirmed(boolean v) { this.guardianIdConfirmed = v; return this; }
        public Builder dataConsentGiven(boolean v) { this.dataConsentGiven = v; return this; }
        public Builder specialCase(boolean v) { this.specialCase = v; return this; }
        public Builder specialCaseNotes(String v) { this.specialCaseNotes = v; return this; }
        public Builder branchId(UUID v) { this.branchId = v; return this; }
        public Builder createdByStaffId(UUID v) { this.createdByStaffId = v; return this; }

        public Patient build() {
            return new Patient(this);
        }
    }
}
