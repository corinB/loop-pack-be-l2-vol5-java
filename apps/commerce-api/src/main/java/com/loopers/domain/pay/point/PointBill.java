package com.loopers.domain.pay.point;

import com.loopers.domain.shared.Money;
import java.time.Instant;

public final class PointBill {
    private final Long id;
    private final long userId;
    private final PointBillType type;
    private final Money amount;
    private final Instant createdAt;

    private PointBill(Long id, long userId, PointBillType type, long amount, Instant createdAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = Money.positive(amount);
        this.createdAt = createdAt;
    }

    public static PointBill charge(long userId, long amount) {
        return new PointBill(null, userId, PointBillType.CHARGE, amount, null);
    }

    public static PointBill restore(long id, long userId, PointBillType type, long amount, Instant createdAt) {
        if (id <= 0 || createdAt == null) {
            throw new IllegalArgumentException("저장된 포인트 기록 상태가 올바르지 않습니다.");
        }
        return new PointBill(id, userId, type, amount, createdAt);
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public PointBillType getType() {
        return type;
    }

    public long getAmount() {
        return amount.getValue();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
