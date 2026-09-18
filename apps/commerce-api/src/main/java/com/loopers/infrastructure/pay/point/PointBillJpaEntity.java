package com.loopers.infrastructure.pay.point;

import com.loopers.domain.pay.point.PointBillType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "point_bills")
public class PointBillJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointBillType type;
    @Column(nullable = false)
    private long amount;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PointBillJpaEntity() {}

    PointBillJpaEntity(long userId, PointBillType type, long amount) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
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
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
