package com.loopers.domain.shopping.repository;

import com.loopers.domain.shopping.model.Like;

// 좋아요 저장소 인터페이스
public interface LikeRepository {
    // 좋아요 저장 (이미 있으면 무시)
    void save(Like like);

    // 좋아요 삭제 (없으면 무시)
    void delete(long userId, long productId);
}
