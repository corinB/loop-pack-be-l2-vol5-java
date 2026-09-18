package com.loopers.domain.shared;

import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.util.Objects;

public final class Money {
    private final long value;

    private Money(long value) {
        if (value < 0) {
            throw new DomainException(DomainErrorCode.INVALID_MONEY);
        }
        this.value = value;
    }

    public static Money zero() {
        return new Money(0L);
    }

    public static Money of(long value) {
        return new Money(value);
    }

    public static Money positive(long value) {
        if (value <= 0) {
            throw new DomainException(DomainErrorCode.NON_POSITIVE_MONEY);
        }
        return new Money(value);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other);
        try {
            return new Money(Math.addExact(value, other.value));
        } catch (ArithmeticException exception) {
            throw new DomainException(DomainErrorCode.CALCULATION_OVERFLOW);
        }
    }

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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Money money)) {
            return false;
        }
        return value == money.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
