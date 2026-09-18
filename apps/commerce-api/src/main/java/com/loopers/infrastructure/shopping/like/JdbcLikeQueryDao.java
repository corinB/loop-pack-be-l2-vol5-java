package com.loopers.infrastructure.shopping.like;

import com.loopers.application.common.PageCriteria;
import com.loopers.application.common.PageResult;
import com.loopers.application.mall.product.BrandSummary;
import com.loopers.application.shopping.like.LikeItem;
import com.loopers.application.shopping.like.LikeQueryDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcLikeQueryDao implements LikeQueryDao {
    private final JdbcClient jdbcClient;

    @Override
    @Transactional(readOnly = true)
    public PageResult<LikeItem> findByUserId(long userId, PageCriteria criteria) {
        long total = jdbcClient.sql("""
                SELECT COUNT(*) FROM product_likes l
                JOIN products p ON p.id = l.product_id
                WHERE l.user_id = :userId AND p.deleted = false
                """)
            .param("userId", userId)
            .query(Long.class)
            .single();

        List<LikeItem> items = jdbcClient.sql("""
                SELECT p.id AS product_id, p.name AS name, p.price AS price,
                       b.id AS brand_id, b.name AS brand_name,
                       COALESCE(c.like_count, 0) AS like_count,
                       l.created_at AS liked_at
                FROM product_likes l
                JOIN products p ON p.id = l.product_id
                JOIN brands b ON b.id = p.brand_id
                LEFT JOIN product_like_counts c ON c.product_id = p.id
                WHERE l.user_id = :userId AND p.deleted = false
                ORDER BY l.created_at DESC, l.product_id DESC
                LIMIT :size OFFSET :offset
                """)
            .param("userId", userId)
            .param("size", criteria.size())
            .param("offset", criteria.offset())
            .query((rs, rowNum) -> new LikeItem(
                rs.getLong("product_id"),
                rs.getString("name"),
                rs.getLong("price"),
                new BrandSummary(rs.getLong("brand_id"), rs.getString("brand_name")),
                rs.getLong("like_count"),
                rs.getTimestamp("liked_at").toInstant()
            ))
            .list();

        return PageResult.of(items, criteria.page(), criteria.size(), total);
    }
}
