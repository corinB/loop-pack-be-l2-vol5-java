package com.loopers.domain.support.error;

public class DomainException extends RuntimeException {
    private final DomainErrorCode errorCode;

    public DomainException(DomainErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public DomainException(DomainErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }
}
