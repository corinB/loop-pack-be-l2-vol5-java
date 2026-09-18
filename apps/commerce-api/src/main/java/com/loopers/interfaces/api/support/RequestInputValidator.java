package com.loopers.interfaces.api.support;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

// 요청 입력값 공통 검증기
public final class RequestInputValidator {
    private RequestInputValidator() {}

    // ID가 양수인지 검증
    public static long requirePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_QUANTITY, fieldName + "는 양의 정수여야 합니다.");
        }
        return id;
    }
}
