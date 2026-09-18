package com.loopers.application.mall.product;

import com.loopers.domain.mall.product.Product;
import java.time.Instant;

// 상품 생성·수정 결과
public record ProductResult(long productId, long brandId, String brandName, String name, String description,
                            long price, int stock, long likeCount, Instant createdAt) {
    public static ProductResult from(Product product, String brandName, long likeCount) {
        return new ProductResult(product.getId(), product.getBrandId(), brandName, product.getName(),
            product.getDescription(), product.getPrice(), product.getStock(), likeCount, product.getCreatedAt());
    }
}
