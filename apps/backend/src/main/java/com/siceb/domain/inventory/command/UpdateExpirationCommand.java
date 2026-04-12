package com.siceb.domain.inventory.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateExpirationCommand(
        UUID itemId,
        LocalDate expirationDate,
        String idempotencyKey
) {}
