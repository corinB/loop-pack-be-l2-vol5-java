package com.loopers.application.mall.product;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.util.Arrays;

public enum ProductSort {
    LATEST("latest"),
    PRICE_ASC("price_asc"),
    LIKES_DESC("likes_desc");

    private final String value;

    ProductSort(String value) {
        this.value = value;
    }

    public static ProductSort from(String value) {
        return Arrays.stream(values())
            .filter(sort -> sort.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new DomainException(DomainErrorCode.INVALID_QUANTITY, "지원하지 않는 상품 정렬입니다."));
    }
}
