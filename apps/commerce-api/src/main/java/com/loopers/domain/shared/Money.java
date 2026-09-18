package com.loopers.domain.shared;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.util.Objects;

// 금액 값 객체
public record Money(long value) {
    public Money {
        if (value < 0) {
            throw new DomainException(DomainErrorCode.INVALID_MONEY);
        }
    }

    public static Money zero() {
        return new Money(0L);
    }

    public static Money of(long value) {
        return new Money(value);
    }

    // 양수 금액 생성
    public static Money positive(long value) {
        if (value <= 0) {
            throw new DomainException(DomainErrorCode.NON_POSITIVE_MONEY);
        }
        return new Money(value);
    }

    // 금액 더하기 (오버플로우 검증 포함)
    public Money add(Money other) {
        Objects.requireNonNull(other);
        try {
            return new Money(Math.addExact(value, other.value));
        } catch (ArithmeticException exception) {
            throw new DomainException(DomainErrorCode.CALCULATION_OVERFLOW);
        }
    }

    // 수량 곱하기 (오버플로우 검증 포함)
    public Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_QUANTITY);
        }
        try {
            return new Money(Math.multiplyExact(value, quantity));
        } catch (ArithmeticException exception) {
            throw new DomainException(DomainErrorCode.CALCULATION_OVERFLOW);
        }
    }

    public long getValue() {
        return value;
    }

}
