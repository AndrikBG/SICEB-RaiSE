package com.siceb.platform.branch.command;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalTime;

public record RegisterBranchCommand(
        @NotBlank String name,
        String address,
        String phone,
        String email,
        LocalTime openingTime,
        LocalTime closingTime,
        String branchCode
) {}
