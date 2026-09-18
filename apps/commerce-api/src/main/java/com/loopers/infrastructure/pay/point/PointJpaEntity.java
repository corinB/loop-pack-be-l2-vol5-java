package com.loopers.infrastructure.pay.point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "points")
public class PointJpaEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false)
    private long balance;

    protected PointJpaEntity() {}

    PointJpaEntity(long userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    void apply(long balance) {
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }
}
