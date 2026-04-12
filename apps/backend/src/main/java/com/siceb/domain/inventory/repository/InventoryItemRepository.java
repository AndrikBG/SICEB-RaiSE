package com.siceb.domain.inventory.repository;

import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.model.InventoryItemId;
import com.siceb.domain.inventory.model.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

    Optional<InventoryItem> findByItemIdAndBranchId(UUID itemId, UUID branchId);

    Page<InventoryItem> findByBranchId(UUID branchId, Pageable pageable);

    Page<InventoryItem> findByBranchIdAndStockStatus(UUID branchId, StockStatus stockStatus, Pageable pageable);

    Page<InventoryItem> findByBranchIdAndCategory(UUID branchId, String category, Pageable pageable);

    Page<InventoryItem> findByBranchIdAndServiceId(UUID branchId, UUID serviceId, Pageable pageable);

    @Query("SELECT i FROM InventoryItem i WHERE i.branchId = :branchId "
            + "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<InventoryItem> searchByNameOrSku(@Param("branchId") UUID branchId,
                                           @Param("term") String term,
                                           Pageable pageable);
}
