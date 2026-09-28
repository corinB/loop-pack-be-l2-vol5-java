package com.loopers.application.shopping.dao;

// 좋아요 수 집계용 DAO
public interface LikeCountAggregationDao {
    void resetAllCounts();

    void aggregateAllCounts();
}
