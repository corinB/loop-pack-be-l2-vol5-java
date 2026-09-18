package com.loopers.application.mall.product;

public interface ProductLikeCountQueryDao {
    long findCount(long productId);
}
