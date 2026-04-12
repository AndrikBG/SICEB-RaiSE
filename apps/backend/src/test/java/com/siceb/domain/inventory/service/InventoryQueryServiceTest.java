package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.model.StockStatus;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    @Mock
    private InventoryItemRepository itemRepository;

    private InventoryQueryService queryService;

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        queryService = new InventoryQueryService(itemRepository);
    }

    @Test
    void findByBranch_paginatesAtDefaultSize() {
        InventoryItem item = createItem("Paracetamol", "PHARMA");
        Page<InventoryItem> page = new PageImpl<>(List.of(item), PageRequest.of(0, 50), 1);
        when(itemRepository.findByBranchId(eq(BRANCH_ID), any(Pageable.class))).thenReturn(page);

        Page<InventoryItem> result = queryService.findByBranch(BRANCH_ID, PageRequest.of(0, 50));

        assertEquals(1, result.getTotalElements());
        assertEquals("Paracetamol", result.getContent().getFirst().getName());
        verify(itemRepository).findByBranchId(BRANCH_ID, PageRequest.of(0, 50));
    }

    @Test
    void findByBranchAndStatus_filtersCorrectly() {
        InventoryItem lowItem = createItem("Insulin", "PHARMA");
        Page<InventoryItem> page = new PageImpl<>(List.of(lowItem));
        when(itemRepository.findByBranchIdAndStockStatus(eq(BRANCH_ID), eq(StockStatus.LOW_STOCK), any(Pageable.class)))
                .thenReturn(page);

        Page<InventoryItem> result = queryService.findByBranchAndStatus(
                BRANCH_ID, StockStatus.LOW_STOCK, PageRequest.of(0, 50));

        assertEquals(1, result.getTotalElements());
        verify(itemRepository).findByBranchIdAndStockStatus(BRANCH_ID, StockStatus.LOW_STOCK, PageRequest.of(0, 50));
    }

    @Test
    void findByBranchAndCategory_delegatesToRepository() {
        InventoryItem item = createItem("Gauze", "SUPPLIES");
        Page<InventoryItem> page = new PageImpl<>(List.of(item));
        when(itemRepository.findByBranchIdAndCategory(eq(BRANCH_ID), eq("SUPPLIES"), any(Pageable.class)))
                .thenReturn(page);

        Page<InventoryItem> result = queryService.findByBranchAndCategory(
                BRANCH_ID, "SUPPLIES", PageRequest.of(0, 50));

        assertEquals(1, result.getTotalElements());
        assertEquals("Gauze", result.getContent().getFirst().getName());
    }

    @Test
    void findByBranchAndService_delegatesToRepository() {
        InventoryItem item = createItem("Scalpel", "INSTRUMENTS");
        Page<InventoryItem> page = new PageImpl<>(List.of(item));
        when(itemRepository.findByBranchIdAndServiceId(eq(BRANCH_ID), eq(SERVICE_ID), any(Pageable.class)))
                .thenReturn(page);

        Page<InventoryItem> result = queryService.findByBranchAndService(
                BRANCH_ID, SERVICE_ID, PageRequest.of(0, 50));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchItems_searchesByNameOrSku() {
        InventoryItem item = createItem("Paracetamol 500mg", "PHARMA");
        Page<InventoryItem> page = new PageImpl<>(List.of(item));
        when(itemRepository.searchByNameOrSku(eq(BRANCH_ID), eq("paracet"), any(Pageable.class)))
                .thenReturn(page);

        Page<InventoryItem> result = queryService.searchItems(BRANCH_ID, "paracet", PageRequest.of(0, 50));

        assertEquals(1, result.getTotalElements());
        assertEquals("Paracetamol 500mg", result.getContent().getFirst().getName());
    }

    @Test
    void findItem_existingItem_returnsItem() {
        UUID itemId = UUID.randomUUID();
        InventoryItem item = createItem("Insulin", "PHARMA");
        when(itemRepository.findByItemIdAndBranchId(itemId, BRANCH_ID)).thenReturn(Optional.of(item));

        Optional<InventoryItem> result = queryService.findItem(itemId, BRANCH_ID);

        assertTrue(result.isPresent());
        assertEquals("Insulin", result.get().getName());
    }

    @Test
    void findItem_notFound_returnsEmpty() {
        UUID itemId = UUID.randomUUID();
        when(itemRepository.findByItemIdAndBranchId(itemId, BRANCH_ID)).thenReturn(Optional.empty());

        Optional<InventoryItem> result = queryService.findItem(itemId, BRANCH_ID);

        assertTrue(result.isEmpty());
    }

    private InventoryItem createItem(String name, String category) {
        return InventoryItem.create(
                UUID.randomUUID(), BRANCH_ID, "SKU-001", name, category,
                SERVICE_ID, "units", 100, 10, null);
    }
}
