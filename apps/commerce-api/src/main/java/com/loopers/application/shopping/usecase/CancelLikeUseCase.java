package com.loopers.application.shopping.usecase;

// 좋아요 취소 유스케이스
public interface CancelLikeUseCase {
    void cancel(long userId, long productId);
}
