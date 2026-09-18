package com.loopers.domain.support.error;

public enum DomainErrorCode {
    INVALID_USER_ID("사용자 ID는 양의 정수여야 합니다."),
    INVALID_MONEY("금액은 0 이상이어야 합니다."),
    NON_POSITIVE_MONEY("금액은 양의 정수여야 합니다."),
    CALCULATION_OVERFLOW("계산 결과가 허용 범위를 초과했습니다."),
    INVALID_STOCK("재고는 0 이상이어야 합니다."),
    INVALID_QUANTITY("수량은 양의 정수여야 합니다."),
    INSUFFICIENT_STOCK("상품 재고가 부족합니다."),
    INVALID_NAME("이름은 앞뒤 공백을 제외하고 1자 이상 100자 이하여야 합니다."),
    INVALID_DESCRIPTION("설명은 1,000자 이하여야 합니다."),
    DELETED_BRAND("브랜드를 찾을 수 없습니다.");

    private final String message;

    DomainErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
