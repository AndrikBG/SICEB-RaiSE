package com.siceb.domain.inventory.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Materialized read model for inventory stock. Updated transactionally by the
 * {@code materialize_inventory_delta()} PG trigger on each delta insert.
 * Application code should NOT update stock fields directly.
 */
@Entity
@Table(name = "inventory_items")
@IdClass(InventoryItemId.class)
public class InventoryItem {

    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Id
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "min_threshold", nullable = false)
    private int minThreshold;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false, length = 20)
    private StockStatus stockStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiration_status", nullable = false, length = 20)
    private ExpirationStatus expirationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {}

    public static InventoryItem create(UUID itemId, UUID branchId, String sku, String name,
                                        String category, UUID serviceId, String unitOfMeasure,
                                        int currentStock, int minThreshold, LocalDate expirationDate) {
        InventoryItem item = new InventoryItem();
        item.itemId = itemId != null ? itemId : UUID.randomUUID();
        item.branchId = branchId;
        item.sku = sku;
        item.name = name;
        item.category = category;
        item.serviceId = serviceId;
        item.unitOfMeasure = unitOfMeasure;
        item.currentStock = currentStock;
        item.minThreshold = minThreshold;
        item.expirationDate = expirationDate;
        item.stockStatus = StockStatus.OK;
        item.expirationStatus = ExpirationStatus.OK;
        item.createdAt = Instant.now();
        item.updatedAt = Instant.now();
        return item;
    }

    public UUID getItemId() { return itemId; }
    public UUID getBranchId() { return branchId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public UUID getServiceId() { return serviceId; }
    public int getCurrentStock() { return currentStock; }
    public int getMinThreshold() { return minThreshold; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public StockStatus getStockStatus() { return stockStatus; }
    public ExpirationStatus getExpirationStatus() { return expirationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
