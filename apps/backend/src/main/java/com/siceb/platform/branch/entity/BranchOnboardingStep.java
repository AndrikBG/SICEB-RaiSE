package com.siceb.platform.branch.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branch_onboarding_status")
public class BranchOnboardingStep {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "step_name", nullable = false, length = 50)
    private String stepName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BranchOnboardingStep() {}

    public BranchOnboardingStep(UUID branchId, String stepName) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.stepName = stepName;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public String getStepName() { return stepName; }
    public String getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }

    // State transitions
    public void markInProgress() {
        this.status = "IN_PROGRESS";
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean needsExecution() {
        return "PENDING".equals(status) || "FAILED".equals(status);
    }
}
