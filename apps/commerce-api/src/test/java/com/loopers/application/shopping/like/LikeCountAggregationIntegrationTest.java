package com.loopers.application.shopping.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.loopers.infrastructure.shopping.like.JdbcLikeCountAggregationDao;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class LikeCountAggregationIntegrationTest {
    @Autowired
    private LikeCountAggregationUseCase aggregationUseCase;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    @MockitoSpyBean
    private JdbcLikeCountAggregationDao aggregationDao;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("전체 관계 COUNT를 저장하고 관계가 사라진 기존 집계는 0으로 갱신한다")
    @Test
    void aggregatesAllCounts_andResetsStaleCount() {
        insertLike(1L, 10L);
        insertLike(2L, 10L);
        jdbcClient.sql("INSERT INTO product_like_counts (product_id, like_count) VALUES (99, 4)").update();

        aggregationUseCase.execute();

        assertThat(findCount(10L)).isEqualTo(2L);
        assertThat(findCount(99L)).isZero();
    }

    @DisplayName("집계 저장 중 실패하면 앞선 0 초기화도 함께 롤백한다")
    @Test
    void rollsBackAllCounts_whenAggregationFails() {
        insertLike(1L, 10L);
        jdbcClient.sql("INSERT INTO product_like_counts (product_id, like_count) VALUES (10, 7)").update();
        doThrow(new IllegalStateException("forced aggregation failure"))
            .when(aggregationDao).aggregateAllCounts();

        try {
            assertThatThrownBy(aggregationUseCase::execute).isInstanceOf(RuntimeException.class);

            assertThat(findCount(10L)).isEqualTo(7L);
        } finally {
            reset(aggregationDao);
        }
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
        return jdbcClient.sql("SELECT like_count FROM product_like_counts WHERE product_id = :productId")
            .param("productId", productId)
            .query(Long.class)
            .single();
    }

}
