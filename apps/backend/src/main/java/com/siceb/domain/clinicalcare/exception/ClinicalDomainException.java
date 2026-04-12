package com.siceb.domain.clinicalcare.exception;

import com.siceb.shared.ErrorCode;

/**
 * Domain exception for clinical care business rule violations.
 * Carries an {@link ErrorCode} for structured API error responses.
 */
public class ClinicalDomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public ClinicalDomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
