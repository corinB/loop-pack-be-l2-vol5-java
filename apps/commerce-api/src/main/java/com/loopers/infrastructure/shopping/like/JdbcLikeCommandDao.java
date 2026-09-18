package com.loopers.infrastructure.shopping.like;

import com.loopers.application.shopping.like.LikeCommandDao;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcLikeCommandDao implements LikeCommandDao {
    private final JdbcClient jdbcClient;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveProduct(long productId) {
        return Boolean.TRUE.equals(jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM products WHERE id = :productId AND deleted = false)")
            .param("productId", productId)
            .query(Boolean.class)
            .single());
    }

    @Override
    @Transactional
    public void register(long userId, long productId) {
        if (existsLike(userId, productId)) {
            return;
        }
        try {
            jdbcClient.sql(
                    "INSERT INTO product_likes (user_id, product_id, created_at) VALUES (:userId, :productId, CURRENT_TIMESTAMP)")
                .param("userId", userId)
                .param("productId", productId)
                .update();
        } catch (DataIntegrityViolationException e) {
            if (!existsLike(userId, productId)) {
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void cancel(long userId, long productId) {
        jdbcClient.sql("DELETE FROM product_likes WHERE user_id = :userId AND product_id = :productId")
            .param("userId", userId)
            .param("productId", productId)
            .update();
    }

    private boolean existsLike(long userId, long productId) {
        return Boolean.TRUE.equals(jdbcClient.sql(
                "SELECT EXISTS(SELECT 1 FROM product_likes WHERE user_id = :userId AND product_id = :productId)")
            .param("userId", userId)
            .param("productId", productId)
            .query(Boolean.class)
            .single());
    }
}
