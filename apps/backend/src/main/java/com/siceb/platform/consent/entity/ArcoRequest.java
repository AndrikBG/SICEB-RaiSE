package com.siceb.platform.consent.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "arco_requests")
public class ArcoRequest {

    @Id
    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private ArcoType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArcoStatus status;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "handled_by")
    private UUID handledBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected ArcoRequest() {}

    public ArcoRequest(UUID patientId, UUID branchId, ArcoType requestType,
                       String description, LocalDate deadline) {
        this.requestId = UUID.randomUUID();
        this.patientId = patientId;
        this.branchId = branchId;
        this.requestType = requestType;
        this.status = ArcoStatus.PENDING;
        this.description = description;
        this.requestedAt = Instant.now();
        this.deadline = deadline;
    }

    public void startProcessing(UUID handledByUserId) {
        this.status = ArcoStatus.IN_PROGRESS;
        this.handledBy = handledByUserId;
    }

    public void complete(String notes) {
        this.status = ArcoStatus.COMPLETED;
        this.resolvedAt = Instant.now();
        this.resolutionNotes = notes;
    }

    public void reject(String notes) {
        this.status = ArcoStatus.REJECTED;
        this.resolvedAt = Instant.now();
        this.resolutionNotes = notes;
    }

    public boolean isOverdue() {
        return (status == ArcoStatus.PENDING || status == ArcoStatus.IN_PROGRESS)
                && LocalDate.now().isAfter(deadline);
    }

    public UUID getRequestId() { return requestId; }
    public UUID getPatientId() { return patientId; }
    public UUID getBranchId() { return branchId; }
    public ArcoType getRequestType() { return requestType; }
    public ArcoStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Instant getRequestedAt() { return requestedAt; }
    public LocalDate getDeadline() { return deadline; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public UUID getHandledBy() { return handledBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum ArcoType {
        ACCESS, RECTIFICATION, CANCELLATION, OPPOSITION
    }

    public enum ArcoStatus {
        PENDING, IN_PROGRESS, COMPLETED, REJECTED
    }
}
