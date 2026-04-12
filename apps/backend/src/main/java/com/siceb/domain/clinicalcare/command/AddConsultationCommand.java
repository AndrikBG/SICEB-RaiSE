package com.siceb.domain.clinicalcare.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddConsultationCommand(
        @NotNull UUID consultationId,
        @NotNull UUID recordId,
        @NotBlank String subjective,
        @NotBlank String objective,
        @NotBlank String diagnosis,
        String diagnosisCode,
        @NotBlank String plan,
        String vitalSigns,
        boolean requiresSupervision,
        UUID supervisorStaffId,
        @NotBlank String idempotencyKey
) {}
