package com.siceb.platform.branch.command;

import java.time.LocalTime;

public record UpdateBranchCommand(
        String name,
        String address,
        String phone,
        String email,
        LocalTime openingTime,
        LocalTime closingTime
) {}
