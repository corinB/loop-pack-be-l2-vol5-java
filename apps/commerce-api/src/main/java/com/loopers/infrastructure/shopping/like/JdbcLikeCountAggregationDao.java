package com.loopers.infrastructure.shopping.like;

import com.loopers.application.shopping.like.LikeCountAggregationDao;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
// 좋아요 수 집계를 처리하는 JDBC DAO
public class JdbcLikeCountAggregationDao implements LikeCountAggregationDao {
    private final JdbcClient jdbcClient;

    // 집계 카운트 전체 초기화
    @Override
    public void resetAllCounts() {
        jdbcClient.sql("UPDATE product_like_counts SET like_count = 0").update();
    }

    // 좋아요 수 전체 재집계
    @Override
    public void aggregateAllCounts() {
        jdbcClient.sql("""
                INSERT INTO product_like_counts (product_id, like_count)
                SELECT product_id, COUNT(*) FROM product_likes GROUP BY product_id
                ON DUPLICATE KEY UPDATE like_count = VALUES(like_count)
                """)
            .update();
    }
}
