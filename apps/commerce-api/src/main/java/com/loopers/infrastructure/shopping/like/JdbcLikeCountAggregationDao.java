package com.loopers.infrastructure.shopping.like;

import com.loopers.application.shopping.like.LikeCountAggregationDao;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcLikeCountAggregationDao implements LikeCountAggregationDao {
    private final JdbcClient jdbcClient;

    @Override
    public void resetAllCounts() {
        jdbcClient.sql("UPDATE product_like_counts SET like_count = 0").update();
    }

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
