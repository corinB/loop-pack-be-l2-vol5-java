package com.loopers.application.shopping.like;

// 좋아요 수 집계용 DAO
public interface LikeCountAggregationDao {
    void resetAllCounts();

    void aggregateAllCounts();
}
