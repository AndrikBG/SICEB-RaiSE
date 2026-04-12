package com.siceb.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "System information endpoints")
public class SystemController {

    @Value("${spring.application.name:siceb-api}")
    private String appName;

    @GetMapping("/info")
    @Operation(summary = "Returns basic system information")
    public Map<String, String> info() {
        return Map.of(
                "name", appName,
                "version", "0.1.0",
                "status", "running"
        );
    }
}
