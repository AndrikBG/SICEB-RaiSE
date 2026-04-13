package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.model.StockStatus;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryItemRepository itemRepository;

    public InventoryQueryService(InventoryItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Page<InventoryItem> findByBranch(UUID branchId, Pageable pageable) {
        return itemRepository.findByBranchId(branchId, pageable);
    }

    public Page<InventoryItem> findByBranchAndStatus(UUID branchId, StockStatus status, Pageable pageable) {
        return itemRepository.findByBranchIdAndStockStatus(branchId, status, pageable);
    }

    public Page<InventoryItem> findByBranchAndCategory(UUID branchId, String category, Pageable pageable) {
        return itemRepository.findByBranchIdAndCategory(branchId, category, pageable);
    }

    public Page<InventoryItem> findByBranchAndService(UUID branchId, UUID serviceId, Pageable pageable) {
        return itemRepository.findByBranchIdAndServiceId(branchId, serviceId, pageable);
    }

    public Page<InventoryItem> searchItems(UUID branchId, String term, Pageable pageable) {
        return itemRepository.searchByNameOrSku(branchId, term, pageable);
    }

    public Optional<InventoryItem> findItem(UUID itemId, UUID branchId) {
        return itemRepository.findByItemIdAndBranchId(itemId, branchId);
    }
}
