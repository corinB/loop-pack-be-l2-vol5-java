package com.loopers.infrastructure.shopping.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.loopers.application.shopping.like.LikeCommandDao;
import com.loopers.domain.mall.product.Product;
import com.loopers.domain.mall.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class JdbcLikeCommandDaoIntegrationTest {
    @Autowired
    private LikeCommandDao likeCommandDao;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcClient jdbcClient;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("활성 상품 확인")
    @Nested
    class ExistsActiveProduct {
        @DisplayName("존재하는 활성 상품이면 true를 반환한다")
        @Test
        void returnsTrue_whenProductIsActive() {
            Product product = productRepository.save(Product.create(1L, "상품", "설명", 1_000L, 5));

            assertThat(likeCommandDao.existsActiveProduct(product.getId())).isTrue();
        }

        @DisplayName("삭제된 상품이면 false를 반환한다")
        @Test
        void returnsFalse_whenProductIsDeleted() {
            Product product = productRepository.save(Product.create(1L, "상품", "설명", 1_000L, 5));
            product.delete();
            productRepository.save(product);

            assertThat(likeCommandDao.existsActiveProduct(product.getId())).isFalse();
        }

        @DisplayName("존재하지 않는 상품이면 false를 반환한다")
        @Test
        void returnsFalse_whenProductDoesNotExist() {
            assertThat(likeCommandDao.existsActiveProduct(999L)).isFalse();
        }
    }

    @DisplayName("좋아요 등록")
    @Nested
    class Register {
        @DisplayName("관계가 없으면 새로 저장한다")
        @Test
        void savesNewRelation() {
            likeCommandDao.register(1L, 10L);

            assertThat(countLikes(1L, 10L)).isEqualTo(1L);
        }

        @DisplayName("이미 등록된 관계는 다시 저장하지 않고 그대로 성공한다")
        @Test
        void ignoresDuplicateRegistration() {
            likeCommandDao.register(1L, 10L);

            likeCommandDao.register(1L, 10L);

            assertThat(countLikes(1L, 10L)).isEqualTo(1L);
        }
    }

    @DisplayName("좋아요 취소")
    @Nested
    class Cancel {
        @DisplayName("존재하는 관계를 제거한다")
        @Test
        void removesExistingRelation() {
            likeCommandDao.register(1L, 10L);

            likeCommandDao.cancel(1L, 10L);

            assertThat(countLikes(1L, 10L)).isZero();
        }

        @DisplayName("존재하지 않는 관계를 취소해도 예외 없이 그대로 성공한다")
        @Test
        void ignoresCancelOfMissingRelation() {
            assertThatNoException().isThrownBy(() -> likeCommandDao.cancel(1L, 10L));
        }
    }

    private long countLikes(long userId, long productId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM product_likes WHERE user_id = :userId AND product_id = :productId")
            .param("userId", userId)
            .param("productId", productId)
            .query(Long.class)
            .single();
    }
}
