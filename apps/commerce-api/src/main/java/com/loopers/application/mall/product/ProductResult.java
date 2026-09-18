package com.loopers.application.mall.product;

import com.loopers.domain.mall.product.Product;
import java.time.Instant;

public record ProductResult(long productId, long brandId, String name, String description, long price, int stock,
                            Instant createdAt) {
    public static ProductResult from(Product product) {
        return new ProductResult(product.getId(), product.getBrandId(), product.getName(), product.getDescription(),
            product.getPrice(), product.getStock(), product.getCreatedAt());
    }
}
