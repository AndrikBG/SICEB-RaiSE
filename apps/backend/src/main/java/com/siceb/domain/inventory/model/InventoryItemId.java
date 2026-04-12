package com.siceb.domain.inventory.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link InventoryItem} (partitioned by branch_id).
 */
public class InventoryItemId implements Serializable {

    private UUID itemId;
    private UUID branchId;

    public InventoryItemId() {}

    public InventoryItemId(UUID itemId, UUID branchId) {
        this.itemId = itemId;
        this.branchId = branchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItemId that)) return false;
        return Objects.equals(itemId, that.itemId) && Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, branchId);
    }
}
