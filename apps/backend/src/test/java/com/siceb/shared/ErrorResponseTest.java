package com.siceb.shared;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void factoryFromErrorCode() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND);
        assertEquals("SICEB-3001", response.code());
        assertEquals("Resource not found", response.message());
        assertNotNull(response.correlationId());
        assertNotNull(response.timestamp());
        assertTrue(response.details().isEmpty());
    }

    @Test
    void factoryWithCustomMessage() {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_FAILED,
                "Patient date of birth is required"
        );
        assertEquals("SICEB-1001", response.code());
        assertEquals("Patient date of birth is required", response.message());
    }

    @Test
    void factoryWithDetails() {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_FAILED,
                "Multiple fields invalid",
                Map.of("email", "invalid format", "name", "must not be blank")
        );
        assertEquals(2, response.details().size());
        assertEquals("invalid format", response.details().get("email"));
    }

    @Test
    void correlationIdIsUnique() {
        ErrorResponse a = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        ErrorResponse b = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        assertNotEquals(a.correlationId(), b.correlationId());
    }

    @Test
    void allErrorCodesHaveUniqueCodes() {
        Set<String> codes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::code)
                .collect(Collectors.toSet());
        assertEquals(ErrorCode.values().length, codes.size(),
                "Duplicate error codes detected");
    }

    @Test
    void allErrorCodesHaveNonBlankMessages() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertFalse(ec.defaultMessage().isBlank(),
                    ec.name() + " has a blank default message");
        }
    }
}
