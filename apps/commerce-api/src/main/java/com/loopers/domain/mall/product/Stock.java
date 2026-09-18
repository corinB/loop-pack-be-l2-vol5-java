package com.loopers.domain.mall.product;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public record Stock(int value) {
    public Stock {
        if (value < 0) {
            throw new DomainException(DomainErrorCode.INVALID_STOCK);
        }
    }

    public static Stock of(int value) {
        return new Stock(value);
    }

    public Stock set(int value) {
        return new Stock(value);
    }

    public Stock decrease(int quantity) {
        if (quantity <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_QUANTITY);
        }
        if (quantity > value) {
            throw new DomainException(DomainErrorCode.INSUFFICIENT_STOCK);
        }
        return new Stock(value - quantity);
    }

    public int getValue() {
        return value;
    }
}
