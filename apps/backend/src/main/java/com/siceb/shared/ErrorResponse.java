package com.siceb.shared;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standardized error envelope for all API error responses.
 * Every error includes a correlation ID for tracing across logs,
 * the error code from {@link ErrorCode}, and optional field-level details.
 */
public record ErrorResponse(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        Map<String, String> details
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.code(),
                errorCode.defaultMessage(),
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.code(),
                message,
                UUID.randomUUID().toString(),
                Instant.now(),
                Map.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, String> details) {
        return new ErrorResponse(
                errorCode.code(),
                message,
                UUID.randomUUID().toString(),
                Instant.now(),
                details
        );
    }
}
