package com.siceb.domain.inventory.service;

import com.siceb.domain.inventory.command.*;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.domain.inventory.model.DeltaType;
import com.siceb.domain.inventory.model.InventoryDelta;
import com.siceb.domain.inventory.model.InventoryItem;
import com.siceb.domain.inventory.repository.InventoryDeltaRepository;
import com.siceb.domain.inventory.repository.InventoryItemRepository;
import com.siceb.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCommandHandlerTest {

    @Mock
    private InventoryItemRepository itemRepository;
    @Mock
    private InventoryDeltaRepository deltaRepository;

    @InjectMocks
    private InventoryCommandHandler handler;

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID STAFF_ID = UUID.randomUUID();

    private InventoryItem testItem;

    @BeforeEach
    void setUp() {
        testItem = InventoryItem.create(
                ITEM_ID, BRANCH_ID, "MED001", "Paracetamol 500mg",
                "medication", UUID.randomUUID(), "units", 100, 20, null);
    }

    @Nested
    class IncrementStock {

        @Test
        void incrementsWithPositiveQuantity() {
            var cmd = new IncrementStockCommand(ITEM_ID, 50, "Delivery", "PO-001", "inc-001");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("inc-001")).thenReturn(false);
            when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertEquals(DeltaType.INCREMENT, result.getDeltaType());
            assertEquals(50, result.getQuantityChange());
            assertNotNull(result.getDeltaId());
        }

        @Test
        void rejectsZeroQuantity() {
            var cmd = new IncrementStockCommand(ITEM_ID, 0, "Bad", null, "inc-002");

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }

        @Test
        void rejectsNegativeQuantity() {
            var cmd = new IncrementStockCommand(ITEM_ID, -5, "Bad", null, "inc-003");

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }
    }

    @Nested
    class DecrementStock {

        @Test
        void decrementsWithPositiveQuantity() {
            var cmd = new DecrementStockCommand(ITEM_ID, 30, "Usage", "DISP-001", "dec-001");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("dec-001")).thenReturn(false);
            when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertEquals(DeltaType.DECREMENT, result.getDeltaType());
            assertEquals(30, result.getQuantityChange());
        }
    }

    @Nested
    class AdjustStock {

        @Test
        void adjustsToAbsoluteQuantity() {
            var cmd = new AdjustStockCommand(ITEM_ID, 75, "Physical count", "adj-001");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("adj-001")).thenReturn(false);
            when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertEquals(DeltaType.ADJUST, result.getDeltaType());
            assertEquals(75, result.getAbsoluteQuantity());
        }

        @Test
        void rejectsNegativeAbsoluteQuantity() {
            var cmd = new AdjustStockCommand(ITEM_ID, -1, "Bad", "adj-002");

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }

        @Test
        void requiresReason() {
            var cmd = new AdjustStockCommand(ITEM_ID, 50, null, "adj-003");

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }
    }

    @Nested
    class SetThreshold {

        @Test
        void setsThreshold() {
            var cmd = new SetThresholdCommand(ITEM_ID, 50, "thr-001");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("thr-001")).thenReturn(false);
            when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertEquals(DeltaType.THRESHOLD, result.getDeltaType());
            assertEquals(50, result.getAbsoluteQuantity());
        }

        @Test
        void rejectsNegativeThreshold() {
            var cmd = new SetThresholdCommand(ITEM_ID, -1, "thr-002");

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }
    }

    @Nested
    class UpdateExpiration {

        @Test
        void updatesExpiration() {
            var cmd = new UpdateExpirationCommand(ITEM_ID, LocalDate.of(2027, 6, 30), "exp-001");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("exp-001")).thenReturn(false);
            when(deltaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertEquals(DeltaType.EXPIRATION, result.getDeltaType());
        }
    }

    @Nested
    class Idempotency {

        @Test
        void returnExistingDeltaOnDuplicateKey() {
            var existing = InventoryDelta.builder()
                    .deltaId(UUID.randomUUID())
                    .itemId(ITEM_ID)
                    .branchId(BRANCH_ID)
                    .deltaType(DeltaType.INCREMENT)
                    .quantityChange(50)
                    .staffId(STAFF_ID)
                    .idempotencyKey("dup-001")
                    .build();

            var cmd = new IncrementStockCommand(ITEM_ID, 50, "Delivery", null, "dup-001");
            when(deltaRepository.existsByIdempotencyKey("dup-001")).thenReturn(true);
            when(deltaRepository.findByIdempotencyKey("dup-001")).thenReturn(Optional.of(existing));

            InventoryDelta result = handler.handle(cmd, BRANCH_ID, STAFF_ID);

            assertSame(existing, result);
            verify(deltaRepository, never()).save(any());
        }
    }

    @Nested
    class ItemNotFound {

        @Test
        void rejectsCommandForMissingItem() {
            var cmd = new IncrementStockCommand(ITEM_ID, 10, "Delivery", null, "inc-nf-001");
            when(deltaRepository.existsByIdempotencyKey("inc-nf-001")).thenReturn(false);
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.empty());

            InventoryException ex = assertThrows(InventoryException.class,
                    () -> handler.handle(cmd, BRANCH_ID, STAFF_ID));
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    class DeltaPersistence {

        @Test
        void persistsDeltaWithAllFields() {
            var cmd = new IncrementStockCommand(ITEM_ID, 50, "Delivery PO-123", "PO-123", "inc-persist");
            when(itemRepository.findByItemIdAndBranchId(ITEM_ID, BRANCH_ID)).thenReturn(Optional.of(testItem));
            when(deltaRepository.existsByIdempotencyKey("inc-persist")).thenReturn(false);

            ArgumentCaptor<InventoryDelta> captor = ArgumentCaptor.forClass(InventoryDelta.class);
            when(deltaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            handler.handle(cmd, BRANCH_ID, STAFF_ID);

            InventoryDelta saved = captor.getValue();
            assertEquals(ITEM_ID, saved.getItemId());
            assertEquals(BRANCH_ID, saved.getBranchId());
            assertEquals(DeltaType.INCREMENT, saved.getDeltaType());
            assertEquals(50, saved.getQuantityChange());
            assertEquals("Delivery PO-123", saved.getReason());
            assertEquals("PO-123", saved.getSourceRef());
            assertEquals(STAFF_ID, saved.getStaffId());
            assertEquals("inc-persist", saved.getIdempotencyKey());
        }
    }
}
