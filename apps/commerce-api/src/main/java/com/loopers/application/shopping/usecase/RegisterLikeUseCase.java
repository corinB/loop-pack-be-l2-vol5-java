package com.loopers.application.shopping.usecase;

// 좋아요 등록 유스케이스
public interface RegisterLikeUseCase {
    void register(long userId, long productId);
}
