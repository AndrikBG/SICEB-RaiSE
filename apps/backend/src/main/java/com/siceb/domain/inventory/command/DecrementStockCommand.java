package com.siceb.domain.inventory.command;

import java.util.UUID;

public record DecrementStockCommand(
        UUID itemId,
        int quantity,
        String reason,
        String sourceRef,
        String idempotencyKey
) {}
