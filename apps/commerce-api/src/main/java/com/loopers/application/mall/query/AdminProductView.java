package com.loopers.application.mall.query;

import com.loopers.application.mall.result.ProductResult;
import java.time.Instant;

// 관리자용 상품 조회 결과
public record AdminProductView(long productId, String name, long price, BrandSummaryView brand, long likeCount,
                           String description, int stock, Instant createdAt) {
    public static AdminProductView from(ProductResult result) {
        return new AdminProductView(result.productId(), result.name(), result.price(),
            new BrandSummaryView(result.brandId(), result.brandName()), result.likeCount(), result.description(),
            result.stock(), result.createdAt());
    }
}
