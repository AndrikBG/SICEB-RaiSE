package com.siceb.platform.branch.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @Column(name = "id")
    private UUID branchId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Branch() {}

    private Branch(Builder builder) {
        this.branchId = UUID.randomUUID();
        this.name = Objects.requireNonNull(builder.name, "Branch name is required");
        this.address = builder.address;
        this.phone = builder.phone;
        this.email = builder.email;
        this.openingTime = builder.openingTime;
        this.closingTime = builder.closingTime;
        this.branchCode = builder.branchCode;
        this.active = true;
        this.onboardingComplete = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public UUID getBranchId() { return branchId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public String getBranchCode() { return branchCode; }
    public boolean isOnboardingComplete() { return onboardingComplete; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters for editable fields
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }
    public void setAddress(String address) { this.address = address; this.updatedAt = Instant.now(); }
    public void setPhone(String phone) { this.phone = phone; this.updatedAt = Instant.now(); }
    public void setEmail(String email) { this.email = email; this.updatedAt = Instant.now(); }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; this.updatedAt = Instant.now(); }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; this.updatedAt = Instant.now(); }

    // Lifecycle
    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
    public void completeOnboarding() { this.onboardingComplete = true; this.updatedAt = Instant.now(); }

    public static class Builder {
        private String name;
        private String address;
        private String phone;
        private String email;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private String branchCode;

        public Builder name(String name) { this.name = name; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder openingTime(LocalTime openingTime) { this.openingTime = openingTime; return this; }
        public Builder closingTime(LocalTime closingTime) { this.closingTime = closingTime; return this; }
        public Builder branchCode(String branchCode) { this.branchCode = branchCode; return this; }

        public Branch build() { return new Branch(this); }
    }
}
