package com.loopers.application.mall.product;

// 상품 좋아요 수 조회
public interface ProductLikeCountQueryDao {
    long findCount(long productId);
}
