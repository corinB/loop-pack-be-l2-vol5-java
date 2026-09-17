package com.loopers.domain.support.error;

public enum DomainErrorCode {
    INVALID_USER_ID("사용자 ID는 양의 정수여야 합니다.");

    private final String message;

    DomainErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
