package com.loopers.application.shopping.like;

public interface LikeCommandDao {
    boolean existsActiveProduct(long productId);

    void register(long userId, long productId);

    void cancel(long userId, long productId);
}
