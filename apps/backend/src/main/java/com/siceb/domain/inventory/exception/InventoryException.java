package com.siceb.domain.inventory.exception;

import com.siceb.shared.ErrorCode;

/**
 * Domain exception for inventory business rule violations.
 * Carries an {@link ErrorCode} for structured API error responses.
 */
public class InventoryException extends RuntimeException {

    private final ErrorCode errorCode;

    public InventoryException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
