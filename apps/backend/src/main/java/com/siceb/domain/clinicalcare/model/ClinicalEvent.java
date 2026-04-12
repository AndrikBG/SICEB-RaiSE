package com.siceb.domain.clinicalcare.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable clinical event — the atomic unit of the append-only event store (IC-02).
 * Once inserted, no UPDATE or DELETE is permitted (enforced at DB trigger + app layer).
 * Each event carries an IdempotencyKey for offline replay deduplication (CRN-43).
 */
@Entity
@Table(name = "clinical_events")
public class ClinicalEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ClinicalEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "performed_by_staff_id", nullable = false)
    private UUID performedByStaffId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    protected ClinicalEvent() {}

    private ClinicalEvent(Builder b) {
        this.eventId = b.eventId;
        this.recordId = b.recordId;
        this.eventType = b.eventType;
        this.occurredAt = b.occurredAt != null ? b.occurredAt : Instant.now();
        this.branchId = b.branchId;
        this.performedByStaffId = b.performedByStaffId;
        this.idempotencyKey = b.idempotencyKey;
        this.payload = b.payload != null ? Map.copyOf(b.payload) : Map.of();
    }

    public UUID getEventId() { return eventId; }
    public UUID getRecordId() { return recordId; }
    public ClinicalEventType getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getBranchId() { return branchId; }
    public UUID getPerformedByStaffId() { return performedByStaffId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Map<String, Object> getPayload() { return payload; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID eventId;
        private UUID recordId;
        private ClinicalEventType eventType;
        private Instant occurredAt;
        private UUID branchId;
        private UUID performedByStaffId;
        private String idempotencyKey;
        private Map<String, Object> payload;

        public Builder eventId(UUID v) { this.eventId = v; return this; }
        public Builder recordId(UUID v) { this.recordId = v; return this; }
        public Builder eventType(ClinicalEventType v) { this.eventType = v; return this; }
        public Builder occurredAt(Instant v) { this.occurredAt = v; return this; }
        public Builder branchId(UUID v) { this.branchId = v; return this; }
        public Builder performedByStaffId(UUID v) { this.performedByStaffId = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }
        public Builder payload(Map<String, Object> v) { this.payload = v; return this; }

        public ClinicalEvent build() {
            return new ClinicalEvent(this);
        }
    }
}
