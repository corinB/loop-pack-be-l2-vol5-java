package com.loopers.application.mall.product;

import java.time.Instant;

// 관리자용 상품 조회 결과
public record AdminProduct(long productId, String name, long price, BrandSummary brand, long likeCount,
                           String description, int stock, Instant createdAt) {
    public static AdminProduct from(ProductResult result) {
        return new AdminProduct(result.productId(), result.name(), result.price(),
            new BrandSummary(result.brandId(), result.brandName()), result.likeCount(), result.description(),
            result.stock(), result.createdAt());
    }
}
