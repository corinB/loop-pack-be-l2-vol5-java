package com.loopers.domain.mall.product;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public final class Stock {
    private int value;

    private Stock(int value) {
        validate(value);
        this.value = value;
    }

    public static Stock of(int value) {
        return new Stock(value);
    }

    public void set(int value) {
        validate(value);
        this.value = value;
    }

    public void decrease(int quantity) {
        if (quantity <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_QUANTITY);
        }
        if (quantity > value) {
            throw new DomainException(DomainErrorCode.INSUFFICIENT_STOCK);
        }
        value -= quantity;
    }

    public int getValue() {
        return value;
    }

    private static void validate(int value) {
        if (value < 0) {
            throw new DomainException(DomainErrorCode.INVALID_STOCK);
        }
    }
}
