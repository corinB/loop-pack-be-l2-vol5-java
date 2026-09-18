package com.loopers.domain.pay.orderbill;

import com.loopers.domain.shared.Money;
import java.time.Instant;

// 주문 결제 완료 기록
public final class OrderBill {
    private final Long id;
    private final long orderId;
    private final long userId;
    private final Money amount;
    private final OrderBillStatus status;
    private final Instant createdAt;

    private OrderBill(Long id, long orderId, long userId, long amount, OrderBillStatus status, Instant createdAt) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("주문 ID는 양수여야 합니다.");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = Money.positive(amount);
        this.status = status;
        this.createdAt = createdAt;
    }

    // 결제 완료 상태로 생성
    public static OrderBill paid(long orderId, long userId, long amount) {
        return new OrderBill(null, orderId, userId, amount, OrderBillStatus.PAID, null);
    }

    // 저장된 데이터로부터 복원
    public static OrderBill restore(long id, long orderId, long userId, long amount, OrderBillStatus status,
                                    Instant createdAt) {
        if (id <= 0 || createdAt == null) {
            throw new IllegalArgumentException("저장된 결제 기록 상태가 올바르지 않습니다.");
        }
        return new OrderBill(id, orderId, userId, amount, status, createdAt);
    }

    public Long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount.getValue();
    }

    public OrderBillStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
