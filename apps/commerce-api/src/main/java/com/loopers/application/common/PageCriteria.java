package com.loopers.application.common;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public record PageCriteria(int page, int size) {
    public PageCriteria {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainException(DomainErrorCode.INVALID_QUANTITY, "페이지는 0 이상, 크기는 1 이상 100 이하여야 합니다.");
        }
    }

    public long offset() {
        return Math.multiplyExact((long) page, size);
    }
}
