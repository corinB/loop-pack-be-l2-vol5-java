package com.loopers.application.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.application.shopping.usecase.LikeCountAggregationUseCase;
import com.loopers.infrastructure.dao.shopping.JdbcLikeCountAggregationDao;
import com.loopers.support.test.IntegrationTest;
import com.loopers.utils.DatabaseCleanUp;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class LikeCountAggregationIntegrationTest {
    @Autowired
    private LikeCountAggregationUseCase aggregationUseCase;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    @Autowired
    private JdbcLikeCountAggregationDao aggregationDao;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("전체 관계 COUNT를 상품에 저장하고 관계가 사라진 기존 값은 0으로 갱신한다")
    @Test
    void aggregatesAllCounts_andResetsStaleCount() {
        insertProduct(10L, 0L);
        insertProduct(99L, 4L);
        insertLike(1L, 10L);
        insertLike(2L, 10L);

        aggregationUseCase.execute();

        assertThat(findCount(10L)).isEqualTo(2L);
        assertThat(findCount(99L)).isZero();
    }

    @DisplayName("증감분 반영은 기존 좋아요 수에 더한다")
    @Test
    void addDeltas_addsToExistingCount() {
        insertProduct(10L, 5L);
        insertProduct(20L, 5L);

        aggregationDao.addDeltas(Map.of(10L, 2L, 20L, -3L));

        assertThat(findCount(10L)).isEqualTo(7L);
        assertThat(findCount(20L)).isEqualTo(2L);
    }

    @DisplayName("증감분 반영은 결과가 음수가 되면 0으로 맞춘다")
    @Test
    void addDeltas_clampsAtZero() {
        insertProduct(10L, 1L);
        insertProduct(30L, 0L);

        aggregationDao.addDeltas(Map.of(10L, -5L, 30L, -2L));

        assertThat(findCount(10L)).isZero();
        assertThat(findCount(30L)).isZero();
    }

    @DisplayName("증감분 반영은 없는 상품 id의 증감분을 무시한다")
    @Test
    void addDeltas_ignoresUnknownProduct() {
        insertProduct(10L, 1L);

        aggregationDao.addDeltas(Map.of(10L, 1L, 404L, 3L));

        assertThat(findCount(10L)).isEqualTo(2L);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM products").query(Long.class).single()).isEqualTo(1L);
    }

    private void insertProduct(long productId, long likeCount) {
        jdbcClient.sql("""
                INSERT INTO products (id, brand_id, name, price, stock, like_count, deleted, created_at, updated_at)
                VALUES (:id, 1, '상품', 1000, 10, :likeCount, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
            .param("id", productId)
            .param("likeCount", likeCount)
            .update();
    }

    private void insertLike(long userId, long productId) {
        jdbcClient.sql("""
                INSERT INTO product_likes (user_id, product_id, created_at)
                VALUES (:userId, :productId, CURRENT_TIMESTAMP)
                """)
            .param("userId", userId)
            .param("productId", productId)
            .update();
    }

    private long findCount(long productId) {
        return jdbcClient.sql("SELECT like_count FROM products WHERE id = :productId")
            .param("productId", productId)
            .query(Long.class)
            .single();
    }
}
