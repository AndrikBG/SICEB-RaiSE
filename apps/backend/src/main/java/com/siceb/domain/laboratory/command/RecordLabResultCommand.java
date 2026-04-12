package com.siceb.domain.laboratory.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordLabResultCommand(
        @NotNull UUID studyId,
        @NotNull UUID resultId,
        @NotBlank String resultText,
        @NotBlank String idempotencyKey
) {}
