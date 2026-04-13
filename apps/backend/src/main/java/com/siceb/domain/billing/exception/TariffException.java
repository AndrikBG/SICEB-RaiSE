package com.siceb.domain.billing.exception;

import com.siceb.shared.ErrorCode;

/**
 * Domain exception for tariff business rule violations.
 * Carries an {@link ErrorCode} for structured API error responses.
 */
public class TariffException extends RuntimeException {

    private final ErrorCode errorCode;

    public TariffException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
