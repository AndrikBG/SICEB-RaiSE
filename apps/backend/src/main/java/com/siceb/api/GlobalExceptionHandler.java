package com.siceb.api;

import com.siceb.domain.clinicalcare.exception.ClinicalDomainException;
import com.siceb.domain.inventory.exception.InventoryException;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.consent.service.LfpdpppComplianceTracker;
import com.siceb.platform.iam.service.AuthenticationService;
import com.siceb.platform.iam.service.IamException;
import com.siceb.shared.ErrorCode;
import com.siceb.shared.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Filter 6: ErrorSanitizer — strips internal details, returns standardized error envelope.
 * Per requeriments3.md §5.4: { code, message, correlationId }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationService.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationService.AuthenticationException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("Authentication error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(BranchException.class)
    public ResponseEntity<ErrorResponse> handleBranch(BranchException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("Branch error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IamException.class)
    public ResponseEntity<ErrorResponse> handleIam(IamException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("IAM error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.FORBIDDEN, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(LfpdpppComplianceTracker.ConsentException.class)
    public ResponseEntity<ErrorResponse> handleConsent(LfpdpppComplianceTracker.ConsentException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("Consent error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<ErrorResponse> handleInventory(InventoryException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("Inventory error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ClinicalDomainException.class)
    public ResponseEntity<ErrorResponse> handleClinicalDomain(ClinicalDomainException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        ErrorResponse response = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        log.warn("Clinical domain error: {} — {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.BRANCH_CONTEXT_REQUIRED, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.put(e.getField(), e.getDefaultMessage()));
        ErrorResponse response = ErrorResponse.of(ErrorCode.VALIDATION_FAILED,
                "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<ErrorResponse> handleJpaSystem(JpaSystemException ex) {
        String rootMsg = ex.getMostSpecificCause().getMessage();
        if (rootMsg != null && rootMsg.contains("Insufficient stock")) {
            String sanitized = rootMsg.lines().findFirst().orElse(rootMsg);
            ErrorResponse response = ErrorResponse.of(ErrorCode.INSUFFICIENT_STOCK, sanitized);
            log.warn("Insufficient stock: {}", rootMsg);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        log.error("JPA system error", ex);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        // ErrorSanitizer: strip internals, return generic message with correlationId
        log.error("Unexpected error", ex);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapToHttpStatus(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_ALREADY_EXISTS, CONFLICT, OPTIMISTIC_LOCK_CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED, INVALID_FORMAT, MISSING_REQUIRED_FIELD -> HttpStatus.BAD_REQUEST;
            case BUSINESS_RULE_VIOLATION, IMMUTABLE_RECORD -> HttpStatus.UNPROCESSABLE_ENTITY;
            case IDEMPOTENCY_KEY_REUSED, INSUFFICIENT_STOCK -> HttpStatus.CONFLICT;
            case BRANCH_CONTEXT_REQUIRED -> HttpStatus.BAD_REQUEST;
            case BRANCH_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED, INVALID_CREDENTIALS, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, RESIDENCY_RESTRICTED, SUPERVISION_REQUIRED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
