package com.loopers.application.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.mall.model.Product;
import com.loopers.domain.mall.repository.ProductRepository;
import com.loopers.domain.shopping.model.Like;
import com.loopers.domain.shopping.repository.LikeRepository;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LikeServiceTest {
    private final LikeRepository likeRepository = mock(LikeRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final LikeService service = new LikeService(likeRepository, productRepository);

    @DisplayName("좋아요 등록")
    @Nested
    class Register {
        @DisplayName("상품이 없으면 PRODUCT_NOT_FOUND이고 저장하지 않는다")
        @Test
        void throwsProductNotFound_whenProductMissing() {
            given(productRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(1L, 10L))
                .isInstanceOfSatisfying(ApplicationException.class,
                    e -> assertThat(e.getErrorCode())
                        .isEqualTo(ApplicationErrorCode.PRODUCT_NOT_FOUND));
            verify(likeRepository, never()).save(any(Like.class));
        }

        @DisplayName("삭제된 상품이면 DELETED_PRODUCT이고 저장하지 않는다")
        @Test
        void throwsDeletedProduct_whenProductDeleted() {
            given(productRepository.findById(10L)).willReturn(Optional.of(product(10L, true)));

            assertThatThrownBy(() -> service.register(1L, 10L))
                .isInstanceOfSatisfying(DomainException.class,
                    e -> assertThat(e.getErrorCode())
                        .isEqualTo(DomainErrorCode.DELETED_PRODUCT));
            verify(likeRepository, never()).save(any(Like.class));
        }

        @DisplayName("활성 상품이면 해당 사용자·상품으로 저장한다")
        @Test
        void savesLike_whenProductActive() {
            given(productRepository.findById(10L)).willReturn(Optional.of(product(10L, false)));

            service.register(1L, 10L);

            ArgumentCaptor<Like> captor = ArgumentCaptor.forClass(Like.class);
            verify(likeRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getProductId()).isEqualTo(10L);
        }
    }

    @DisplayName("좋아요 취소")
    @Nested
    class Cancel {
        @DisplayName("상품을 조회하지 않고 삭제를 호출한다")
        @Test
        void deletesWithoutLookingUpProduct() {
            service.cancel(1L, 10L);

            verify(likeRepository).delete(1L, 10L);
            verifyNoInteractions(productRepository);
        }
    }

    private Product product(long id, boolean deleted) {
        return Product.restore(id, 1L, "상품", null, 1_000L, 100, deleted, Instant.now());
    }
}
