package com.loopers.application.mall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.loopers.application.mall.command.ProductCommand;
import com.loopers.application.support.error.ApplicationErrorCode;
import com.loopers.application.support.error.ApplicationException;
import com.loopers.domain.mall.model.Brand;
import com.loopers.domain.mall.model.Product;
import com.loopers.domain.mall.repository.BrandRepository;
import com.loopers.domain.mall.repository.ProductRepository;
import com.loopers.domain.support.error.DomainErrorCode;
import com.loopers.domain.support.error.DomainException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final ProductService service = new ProductService(productRepository, brandRepository);

    @DisplayName("상품 등록")
    @Nested
    class Create {
        private final ProductCommand.Create command = new ProductCommand.Create(1L, "상품", null, 1_000L, 5);

        @DisplayName("브랜드를 공유 잠금으로 읽고 상품을 저장한다")
        @Test
        void readsBrandWithShareLock_andSavesProduct() {
            given(brandRepository.findByIdForShare(1L))
                .willReturn(Optional.of(Brand.restore(1L, "브랜드", null, false, Instant.now())));
            given(productRepository.save(any(Product.class)))
                .willReturn(Product.restore(7L, 1L, "상품", null, 1_000L, 5, false, Instant.now()));

            long productId = service.execute(command);

            assertThat(productId).isEqualTo(7L);
            verify(brandRepository).findByIdForShare(1L);
            verify(brandRepository, never()).findById(anyLong());
        }

        @DisplayName("삭제된 브랜드면 DELETED_BRAND로 거절하고 저장하지 않는다")
        @Test
        void rejectsCreate_whenBrandDeleted() {
            given(brandRepository.findByIdForShare(1L))
                .willReturn(Optional.of(Brand.restore(1L, "브랜드", null, true, Instant.now())));

            assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.DELETED_BRAND);
            verify(productRepository, never()).save(any(Product.class));
        }

        @DisplayName("없는 브랜드면 BRAND_NOT_FOUND로 거절하고 저장하지 않는다")
        @Test
        void rejectsCreate_whenBrandNotFound() {
            given(brandRepository.findByIdForShare(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.BRAND_NOT_FOUND);
            verify(productRepository, never()).save(any(Product.class));
        }
    }
}
