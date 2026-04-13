package com.siceb.domain.inventory.command;

import java.util.UUID;

public record SetThresholdCommand(
        UUID itemId,
        int threshold,
        String idempotencyKey
) {}
