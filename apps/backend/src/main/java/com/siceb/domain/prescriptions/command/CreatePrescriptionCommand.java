package com.siceb.domain.prescriptions.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePrescriptionCommand(
        @NotNull UUID prescriptionId,
        @NotNull UUID consultationId,
        @NotNull UUID recordId,
        @NotEmpty List<PrescriptionItemDto> items,
        @NotBlank String idempotencyKey
) {
    public record PrescriptionItemDto(
            @NotNull UUID medicationId,
            @NotBlank String medicationName,
            @NotNull Integer quantity,
            @NotBlank String dosage,
            @NotBlank String frequency,
            String duration,
            String route,
            String instructions,
            boolean isControlled // Phase 5: validated against medication catalog
    ) {}
}
