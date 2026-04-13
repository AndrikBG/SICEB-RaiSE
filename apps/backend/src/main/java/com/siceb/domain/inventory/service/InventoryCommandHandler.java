package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.command.*;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.domain.inventory.model.DeltaType;
import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.repository.InventoryDeltaRepository;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processes inventory delta commands. Validates input, checks idempotency,
 * verifies item existence, and persists the delta. Stock materialization
 * happens in the PG trigger (same transaction).
 */
@Service
public class InventoryCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandHandler.class);

    private final InventoryItemRepository itemRepository;
    private final InventoryDeltaRepository deltaRepository;

    public InventoryCommandHandler(InventoryItemRepository itemRepository,
                                    InventoryDeltaRepository deltaRepository) {
        this.itemRepository = itemRepository;
        this.deltaRepository = deltaRepository;
    }

    @Transactional
    public InventoryDelta handle(IncrementStockCommand cmd, UUID branchId, UUID staffId) {
        validatePositiveQuantity(cmd.quantity());
        return processDelta(cmd.idempotencyKey(), cmd.itemId(), branchId, staffId,
                DeltaType.INCREMENT, cmd.quantity(), null, cmd.reason(), cmd.sourceRef());
    }

    @Transactional
    public InventoryDelta handle(DecrementStockCommand cmd, UUID branchId, UUID staffId) {
        validatePositiveQuantity(cmd.quantity());
        return processDelta(cmd.idempotencyKey(), cmd.itemId(), branchId, staffId,
                DeltaType.DECREMENT, cmd.quantity(), null, cmd.reason(), cmd.sourceRef());
    }

    @Transactional
    public InventoryDelta handle(AdjustStockCommand cmd, UUID branchId, UUID staffId) {
        if (cmd.absoluteQuantity() < 0) {
            throw new InventoryException(ErrorCode.VALIDATION_FAILED,
                    "Absolute quantity must be non-negative");
        }
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw new InventoryException(ErrorCode.VALIDATION_FAILED,
                    "Reason is required for stock adjustments");
        }
        return processDelta(cmd.idempotencyKey(), cmd.itemId(), branchId, staffId,
                DeltaType.ADJUST, null, cmd.absoluteQuantity(), cmd.reason(), null);
    }

    @Transactional
    public InventoryDelta handle(SetThresholdCommand cmd, UUID branchId, UUID staffId) {
        if (cmd.threshold() < 0) {
            throw new InventoryException(ErrorCode.VALIDATION_FAILED,
                    "Threshold must be non-negative");
        }
        return processDelta(cmd.idempotencyKey(), cmd.itemId(), branchId, staffId,
                DeltaType.THRESHOLD, null, cmd.threshold(), null, null);
    }

    @Transactional
    public InventoryDelta handle(UpdateExpirationCommand cmd, UUID branchId, UUID staffId) {
        // absoluteQuantity is overloaded to carry the epoch day for EXPIRATION deltas
        // The PG trigger interprets this field as a date cast
        Integer epochDay = cmd.expirationDate() != null
                ? (int) cmd.expirationDate().toEpochDay()
                : null;
        return processDelta(cmd.idempotencyKey(), cmd.itemId(), branchId, staffId,
                DeltaType.EXPIRATION, null, epochDay, null, null);
    }

    private InventoryDelta processDelta(String idempotencyKey, UUID itemId, UUID branchId,
                                         UUID staffId, DeltaType deltaType,
                                         Integer quantityChange, Integer absoluteQuantity,
                                         String reason, String sourceRef) {
        // Idempotency check
        if (deltaRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Duplicate idempotency key: {}", idempotencyKey);
            return deltaRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new InventoryException(ErrorCode.INTERNAL_ERROR,
                            "Idempotency key exists but delta not found"));
        }

        // Verify item exists in this branch
        itemRepository.findByItemIdAndBranchId(itemId, branchId)
                .orElseThrow(() -> new InventoryException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Inventory item not found: " + itemId));

        // Build and persist delta — trigger materializes stock
        InventoryDelta delta = InventoryDelta.builder()
                .itemId(itemId)
                .branchId(branchId)
                .deltaType(deltaType)
                .quantityChange(quantityChange)
                .absoluteQuantity(absoluteQuantity)
                .reason(reason)
                .sourceRef(sourceRef)
                .staffId(staffId)
                .idempotencyKey(idempotencyKey)
                .build();

        return deltaRepository.save(delta);
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InventoryException(ErrorCode.VALIDATION_FAILED,
                    "Quantity must be positive");
        }
    }
}
