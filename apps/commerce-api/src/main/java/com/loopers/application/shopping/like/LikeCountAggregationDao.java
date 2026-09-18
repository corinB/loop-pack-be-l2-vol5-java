package com.loopers.application.shopping.like;

public interface LikeCountAggregationDao {
    void resetAllCounts();

    void aggregateAllCounts();
}
