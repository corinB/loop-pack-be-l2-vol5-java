package com.loopers.domain.shopping.like;

import java.time.Instant;

public final class Like {
    private final Long id;
    private final long userId;
    private final long productId;
    private final Instant createdAt;

    private Like(Long id, long userId, long productId, Instant createdAt) {
        if (userId <= 0 || productId <= 0) {
            throw new IllegalArgumentException("사용자 ID와 상품 ID는 양수여야 합니다.");
        }
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public static Like create(long userId, long productId) {
        return new Like(null, userId, productId, null);
    }

    public static Like restore(long id, long userId, long productId, Instant createdAt) {
        if (id <= 0 || createdAt == null) {
            throw new IllegalArgumentException("저장된 좋아요 상태가 올바르지 않습니다.");
        }
        return new Like(id, userId, productId, createdAt);
    }

    public Long getId() { return id; }
    public long getUserId() { return userId; }
    public long getProductId() { return productId; }
    public Instant getCreatedAt() { return createdAt; }
}
