package com.siceb.shared;

/**
 * Standardized error code catalog for all API responses.
 * Codes follow the pattern SICEB-XNNN where X is the category
 * and NNN is the sequential number within that category.
 */
public enum ErrorCode {

    // 0xxx — System
    INTERNAL_ERROR("SICEB-0001", "Internal server error"),
    SERVICE_UNAVAILABLE("SICEB-0002", "Service temporarily unavailable"),

    // 1xxx — Validation
    VALIDATION_FAILED("SICEB-1001", "Request validation failed"),
    INVALID_FORMAT("SICEB-1002", "Invalid data format"),
    MISSING_REQUIRED_FIELD("SICEB-1003", "Required field is missing"),

    // 2xxx — Authentication & Authorization
    UNAUTHORIZED("SICEB-2001", "Authentication required"),
    INVALID_CREDENTIALS("SICEB-2002", "Invalid credentials"),
    TOKEN_EXPIRED("SICEB-2003", "Authentication token expired"),
    FORBIDDEN("SICEB-2004", "Insufficient permissions"),

    // 3xxx — Resource
    RESOURCE_NOT_FOUND("SICEB-3001", "Resource not found"),
    RESOURCE_ALREADY_EXISTS("SICEB-3002", "Resource already exists"),

    // 4xxx — Conflict & Concurrency
    CONFLICT("SICEB-4001", "Resource conflict"),
    IDEMPOTENCY_KEY_REUSED("SICEB-4002", "Idempotency key already processed"),
    OPTIMISTIC_LOCK_CONFLICT("SICEB-4003", "Resource modified by another operation"),

    RESIDENCY_RESTRICTED("SICEB-2005", "Action restricted by residency level policy"),
    SUPERVISION_REQUIRED("SICEB-2006", "Action requires supervisor oversight"),

    // 5xxx — Multi-tenant
    BRANCH_CONTEXT_REQUIRED("SICEB-5001", "Active branch context is required"),
    BRANCH_ACCESS_DENIED("SICEB-5002", "User not assigned to the specified branch"),

    // 6xxx — Business Rules
    BUSINESS_RULE_VIOLATION("SICEB-6001", "Business rule violation"),
    INSUFFICIENT_STOCK("SICEB-6002", "Insufficient inventory stock"),
    IMMUTABLE_RECORD("SICEB-6003", "Record cannot be modified");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
