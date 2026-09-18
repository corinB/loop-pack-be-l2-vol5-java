package com.loopers.infrastructure.shopping.like;

import com.loopers.domain.shopping.like.Like;
import org.springframework.stereotype.Component;

@Component
public class LikeEntityMapper {
    public LikeJpaEntity toNewEntity(Like like) {
        return new LikeJpaEntity(like.getUserId(), like.getProductId());
    }

    public Like toDomain(LikeJpaEntity entity) {
        return Like.restore(entity.getId(), entity.getUserId(), entity.getProductId(), entity.getCreatedAt());
    }
}
