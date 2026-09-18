package com.loopers.application.shopping.like;

// 좋아요 등록/취소용 커맨드 DAO
public interface LikeCommandDao {
    boolean existsActiveProduct(long productId);

    void register(long userId, long productId);

    void cancel(long userId, long productId);
}
