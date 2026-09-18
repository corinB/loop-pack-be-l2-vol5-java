package com.loopers.infrastructure.mall.product;

import com.loopers.application.mall.product.ProductLikeCountQueryDao;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
// JDBC 기반 상품 좋아요 수 조회
public class JdbcProductLikeCountQueryDao implements ProductLikeCountQueryDao {
    private final JdbcClient jdbcClient;

    // 상품 좋아요 수 조회
    @Override
    @Transactional(readOnly = true)
    public long findCount(long productId) {
        return jdbcClient.sql("SELECT like_count FROM product_like_counts WHERE product_id = :productId")
            .param("productId", productId)
            .query(Long.class)
            .optional()
            .orElse(0L);
    }
}
