package com.loopers.application.support.error;

public enum ApplicationErrorCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    BRAND_NOT_FOUND("브랜드를 찾을 수 없습니다."),
    BRAND_HAS_ACTIVE_PRODUCTS("활성 상품이 연결된 브랜드는 삭제할 수 없습니다.");

    private final String message;

    ApplicationErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
