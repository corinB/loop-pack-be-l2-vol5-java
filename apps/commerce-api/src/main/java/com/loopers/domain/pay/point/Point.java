package com.loopers.domain.pay.point;

import com.loopers.domain.shared.Money;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;

public final class Point {
    private final long userId;
    private Money balance;

    private Point(long userId, Money balance) {
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
        this.userId = userId;
        this.balance = balance;
    }

    public static Point zero(long userId) {
        return new Point(userId, Money.zero());
    }

    public static Point restore(long userId, long balance) {
        return new Point(userId, Money.of(balance));
    }

    public void charge(Money amount) {
        balance = balance.add(amount);
    }

    public void use(Money amount) {
        if (amount.getValue() > balance.getValue()) {
            throw new DomainException(DomainErrorCode.INSUFFICIENT_POINT);
        }
        balance = Money.of(balance.getValue() - amount.getValue());
    }

    public long getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance.getValue();
    }
}
