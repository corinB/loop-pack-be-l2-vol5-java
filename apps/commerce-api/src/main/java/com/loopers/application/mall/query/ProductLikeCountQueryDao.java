package com.loopers.application.mall.query;

// 상품 좋아요 수 조회
public interface ProductLikeCountQueryDao {
    long findCount(long productId);
}
