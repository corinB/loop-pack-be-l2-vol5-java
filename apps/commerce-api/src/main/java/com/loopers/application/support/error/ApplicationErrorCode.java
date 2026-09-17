package com.loopers.application.support.error;

public enum ApplicationErrorCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.");

    private final String message;

    ApplicationErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
