package com.siceb.domain.inventory.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, append-only inventory delta event (IC-02 pattern).
 * Once inserted, no UPDATE or DELETE is permitted (enforced by PG triggers).
 * Each delta carries an idempotency key for deduplication.
 * The {@code materialize_inventory_delta()} trigger materializes stock on INSERT.
 */
@Entity
@Table(name = "inventory_deltas")
@IdClass(InventoryDeltaId.class)
public class InventoryDelta {

    @Id
    @Column(name = "delta_id")
    private UUID deltaId;

    @Id
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delta_type", nullable = false, length = 20)
    private DeltaType deltaType;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Column(name = "absolute_quantity")
    private Integer absoluteQuantity;

    @Column(name = "reason")
    private String reason;

    @Column(name = "source_ref", length = 200)
    private String sourceRef;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryDelta() {}

    private InventoryDelta(Builder b) {
        this.deltaId = b.deltaId != null ? b.deltaId : UUID.randomUUID();
        this.branchId = b.branchId;
        this.itemId = b.itemId;
        this.deltaType = b.deltaType;
        this.quantityChange = b.quantityChange;
        this.absoluteQuantity = b.absoluteQuantity;
        this.reason = b.reason;
        this.sourceRef = b.sourceRef;
        this.staffId = b.staffId;
        this.idempotencyKey = b.idempotencyKey;
        this.createdAt = Instant.now();
    }

    public UUID getDeltaId() { return deltaId; }
    public UUID getBranchId() { return branchId; }
    public UUID getItemId() { return itemId; }
    public DeltaType getDeltaType() { return deltaType; }
    public Integer getQuantityChange() { return quantityChange; }
    public Integer getAbsoluteQuantity() { return absoluteQuantity; }
    public String getReason() { return reason; }
    public String getSourceRef() { return sourceRef; }
    public UUID getStaffId() { return staffId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID deltaId;
        private UUID branchId;
        private UUID itemId;
        private DeltaType deltaType;
        private Integer quantityChange;
        private Integer absoluteQuantity;
        private String reason;
        private String sourceRef;
        private UUID staffId;
        private String idempotencyKey;

        public Builder deltaId(UUID v) { this.deltaId = v; return this; }
        public Builder branchId(UUID v) { this.branchId = v; return this; }
        public Builder itemId(UUID v) { this.itemId = v; return this; }
        public Builder deltaType(DeltaType v) { this.deltaType = v; return this; }
        public Builder quantityChange(Integer v) { this.quantityChange = v; return this; }
        public Builder absoluteQuantity(Integer v) { this.absoluteQuantity = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder sourceRef(String v) { this.sourceRef = v; return this; }
        public Builder staffId(UUID v) { this.staffId = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }

        public InventoryDelta build() {
            return new InventoryDelta(this);
        }
    }
}
