package com.loopers.infrastructure.persistence.mall.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.loopers.domain.mall.model.Product;
import com.loopers.domain.mall.repository.ProductRepository;
import com.loopers.support.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
class ProductRepositoryIntegrationTest {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EntityManager entityManager;

    @DisplayName("상품을 저장하고 수정한 뒤 영속성 컨텍스트를 비워도 상태를 보존한다")
    @Test
    @Transactional
    void savesAndUpdatesProduct() {
        Product product = productRepository.save(Product.create(1L, "상품", "설명", 1_000L, 5));
        product.update("변경", null, 2_000L);
        product.setStock(0);
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        Product restored = productRepository.findById(product.getId()).orElseThrow();

        assertThat(restored.getName()).isEqualTo("변경");
        assertThat(restored.getDescription()).isNull();
        assertThat(restored.getPrice()).isEqualTo(2_000L);
        assertThat(restored.getStock()).isZero();
        assertThat(restored.getCreatedAt()).isCloseTo(product.getCreatedAt(), within(1, java.time.temporal.ChronoUnit.MICROS));
    }

    @DisplayName("새 상품의 좋아요 수는 0이고, 상품을 수정해 저장해도 외부에서 갱신한 좋아요 수를 덮어쓰지 않는다")
    @Test
    @Transactional
    void keepsLikeCountWrittenOutsideJpa() {
        Product product = productRepository.save(Product.create(1L, "상품", "설명", 1_000L, 5));
        entityManager.flush();
        entityManager.clear();
        assertThat(likeCountOf(product.getId())).isZero();

        entityManager.createNativeQuery("UPDATE products SET like_count = 5 WHERE id = :id")
            .setParameter("id", product.getId())
            .executeUpdate();
        Product loaded = productRepository.findById(product.getId()).orElseThrow();
        loaded.setStock(0);
        productRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        assertThat(likeCountOf(product.getId())).isEqualTo(5L);
    }

    private long likeCountOf(Long productId) {
        return ((Number) entityManager.createNativeQuery("SELECT like_count FROM products WHERE id = :id")
            .setParameter("id", productId)
            .getSingleResult()).longValue();
    }
}
