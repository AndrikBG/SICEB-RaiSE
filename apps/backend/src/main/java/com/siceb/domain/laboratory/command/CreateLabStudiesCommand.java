package com.siceb.domain.laboratory.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateLabStudiesCommand(
        @NotNull UUID consultationId,
        @NotNull UUID recordId,
        @NotEmpty List<LabStudyItem> studies,
        @NotBlank String idempotencyKey
) {
    public record LabStudyItem(
            @NotNull UUID studyId,
            @NotBlank String studyType,
            String priority,
            String instructions
    ) {}
}
