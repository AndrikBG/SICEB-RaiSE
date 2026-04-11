package com.siceb.platform.branch.exception;

import com.siceb.shared.ErrorCode;

public class BranchException extends RuntimeException {

    private final ErrorCode errorCode;

    public BranchException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
