package com.siceb.domain.inventory.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link InventoryDelta} (partitioned by branch_id).
 */
public class InventoryDeltaId implements Serializable {

    private UUID deltaId;
    private UUID branchId;

    public InventoryDeltaId() {}

    public InventoryDeltaId(UUID deltaId, UUID branchId) {
        this.deltaId = deltaId;
        this.branchId = branchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryDeltaId that)) return false;
        return Objects.equals(deltaId, that.deltaId) && Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deltaId, branchId);
    }
}
