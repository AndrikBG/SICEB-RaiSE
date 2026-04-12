package com.siceb.domain.laboratory.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model entity for the pending lab studies work queue (US-040).
 * Populated from LAB_ORDER and LAB_RESULT clinical events.
 */
@Entity
@Table(name = "pending_lab_studies_view")
public class PendingLabStudy {

    @Id
    @Column(name = "study_id")
    private UUID studyId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "patient_name", nullable = false, length = 300)
    private String patientName;

    @Column(name = "consultation_id")
    private UUID consultationId;

    @Column(name = "study_type", nullable = false, length = 100)
    private String studyType;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StudyStatus status;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by_staff", nullable = false)
    private UUID requestedByStaff;

    @Column(name = "result_text")
    private String resultText;

    @Column(name = "result_recorded_at")
    private Instant resultRecordedAt;

    @Column(name = "result_recorded_by")
    private UUID resultRecordedBy;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    protected PendingLabStudy() {}

    public PendingLabStudy(UUID studyId, UUID eventId, UUID recordId, UUID patientId,
                           String patientName, UUID consultationId, String studyType,
                           String priority, String instructions,
                           Instant requestedAt, UUID requestedByStaff, UUID branchId) {
        this.studyId = studyId;
        this.eventId = eventId;
        this.recordId = recordId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.consultationId = consultationId;
        this.studyType = studyType;
        this.priority = priority;
        this.status = StudyStatus.PENDING;
        this.instructions = instructions;
        this.requestedAt = requestedAt;
        this.requestedByStaff = requestedByStaff;
        this.branchId = branchId;
    }

    public void recordResult(String resultText, UUID recordedBy) {
        this.status = StudyStatus.COMPLETED;
        this.resultText = resultText;
        this.resultRecordedAt = Instant.now();
        this.resultRecordedBy = recordedBy;
    }

    // Getters
    public UUID getStudyId() { return studyId; }
    public UUID getEventId() { return eventId; }
    public UUID getRecordId() { return recordId; }
    public UUID getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public UUID getConsultationId() { return consultationId; }
    public String getStudyType() { return studyType; }
    public String getPriority() { return priority; }
    public StudyStatus getStatus() { return status; }
    public String getInstructions() { return instructions; }
    public Instant getRequestedAt() { return requestedAt; }
    public UUID getRequestedByStaff() { return requestedByStaff; }
    public String getResultText() { return resultText; }
    public Instant getResultRecordedAt() { return resultRecordedAt; }
    public UUID getResultRecordedBy() { return resultRecordedBy; }
    public UUID getBranchId() { return branchId; }
}
