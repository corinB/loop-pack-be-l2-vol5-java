package com.loopers.application.support.error;

public class ApplicationException extends RuntimeException {
    private final ApplicationErrorCode errorCode;

    public ApplicationException(ApplicationErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public ApplicationException(ApplicationErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationErrorCode getErrorCode() {
        return errorCode;
    }
}
