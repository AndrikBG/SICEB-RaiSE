package com.siceb.domain.inventory.command;

import java.util.UUID;

public record AdjustStockCommand(
        UUID itemId,
        int absoluteQuantity,
        String reason,
        String idempotencyKey
) {}
