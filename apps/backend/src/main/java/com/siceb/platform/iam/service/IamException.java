package com.siceb.platform.iam.service;

import com.siceb.shared.ErrorCode;

public class IamException extends RuntimeException {
    private final ErrorCode errorCode;

    public IamException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

